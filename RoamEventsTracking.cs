using System.Collections.Generic;
using System;
using System.IO;
using System.Linq;
using System.Text;
using System.Net.WebSockets;
using System.Threading;
using System.Threading.Tasks;

namespace Oxide.Plugins
{
    [Info("Roam Events Tracking", "Jack Chapman", "1.0.0")]
    [Description("Tracks kills and aks for event")]

    public class RoamEventsTracking : CovalencePlugin
    {
        private readonly ClientWebSocket _client = new ClientWebSocket();
        private readonly Dictionary<ulong, double> _deposit = new Dictionary<ulong, double>();
        private readonly object _messageLock = new object();
        private Dictionary<ulong, string> _teams = new Dictionary<ulong, string>();
        private void Loaded()
        {
            _client.Options.SetRequestHeader("Authorization", "Basic " + Convert.ToBase64String(Encoding.UTF8.GetBytes(":" + Config["WebsocketCredentials"])));
            Task.Run(async () =>
            {
                await Connect();
                _teams = await ReceiveTeams();
            });
        }

        private async Task<Dictionary<ulong, string>> ReceiveTeams()
        {
            var teams = new Dictionary<ulong, string>();
            var buffer = new ArraySegment<byte>(new byte[2048]);
            using (var ms = new MemoryStream())
            {
                WebSocketReceiveResult result;
                do
                {
                    result = await _client.ReceiveAsync(buffer, CancellationToken.None);
                    ms.Write(buffer.Array, buffer.Offset, result.Count);
                } while (!result.EndOfMessage);
        
                ms.Seek(0, SeekOrigin.Begin);
                using (var reader = new StreamReader(ms, Encoding.UTF8))
                {
                    var teamStrings = (await reader.ReadToEndAsync()).Split('\n');
                    var teamSize = int.Parse(teamStrings[0]);
                    foreach (var team in teamStrings.Skip(1))
                    {
                        var members = team.Split(new[] {' '}, teamSize + 1);
                        var teamName = members.Last();
                        foreach (var m in members.SkipLast(1))
                        {
                            teams[ulong.Parse(m)] = teamName;
                        }
                    }
                }
            }
        
            return teams;
        }

        private void Unload()
        {
            Task.Run(async () => await _client.CloseAsync(WebSocketCloseStatus.NormalClosure, "", CancellationToken.None));
        }
        
        protected override void LoadDefaultConfig()
        {
            Config["WebsocketAddress"] = "CHANGE THIS";
            Config["WebsocketCredentials"] = "CHANGE THIS";
        }

        private object OnPlayerDeath(BasePlayer player, HitInfo info)
        {
            if (info.InitiatorPlayer == null) return null;
            if (!_teams.ContainsKey(info.InitiatorPlayer.userID)) return null;
            SendMsg(_teams[info.InitiatorPlayer.userID] + $"\nkill\n{info.InitiatorPlayer.displayName} {player.displayName}");
            return null;
        }

        private void OnPlayerLootEnd(PlayerLoot inventory)
        {
            var player = inventory.baseEntity.userID;
            if (!_deposit.ContainsKey(player) || !_teams.ContainsKey(player)) return;
            var team = _teams[player];
            SendMsg($"{team}\ngun\n{(int)_deposit[player]}");
            _deposit.Remove(player);
        }
        
        private ItemContainer.CanAcceptResult? CanAcceptItem(ItemContainer container, Item item, int targetPos)
        {
            if (item.info.shortname != "rifle.ak") return null;
            
            if (item.parent == null && container == null)
            {
                return ItemContainer.CanAcceptResult.CannotAcceptRightNow;
            }

            var toContainer = ContainerOwner(container);
            var fromContainer = ContainerOwner(item.parent);
            if (toContainer == null || fromContainer == null) return null;

            var positive = 1.0;
            BuildingPrivlidge priv;
            ulong player;
            if (IsGunStorage(toContainer) && fromContainer is BasePlayer)
            {
                priv = toContainer.GetBuildingPrivilege();
                player = (fromContainer as BasePlayer).userID;
            }
            else if (toContainer is BasePlayer && IsGunStorage(fromContainer))
            {
                priv = fromContainer.GetBuildingPrivilege();
                player = (toContainer as BasePlayer).userID;
                positive = -1;
                if(targetPos == -1) positive /= 2.0; // todo needs testing
            }
            else return null;

            if (priv == null) return null;
            _deposit[player] = (_deposit.ContainsKey(player) ? _deposit[player] : 0) + positive;
            return null;
        }

        private static bool IsGunStorage(BaseEntity entity)
        {
            return entity is BoxStorage || entity is VendingMachine;
        }
        
        private static BaseEntity ContainerOwner(ItemContainer container)
        {
            if (container == null) return null;
            if (container.entityOwner != null) return container.entityOwner;
            return container.playerOwner != null ? container.playerOwner : null;
        }

        private async Task Connect()
        {
            if(_client.State != WebSocketState.Open)
                await _client.ConnectAsync(new Uri(Config["WebsocketAddress"].ToString()), CancellationToken.None);
        }

        private void SendMsg(string data) {
          lock(_messageLock)
          {
              Task.Run(async () =>
              {
                  await Connect();
                  await _client.SendAsync(new ArraySegment<byte>(Encoding.UTF8.GetBytes(data)),
                      WebSocketMessageType.Text, true, CancellationToken.None);
              });
          }
        }
    }
}
