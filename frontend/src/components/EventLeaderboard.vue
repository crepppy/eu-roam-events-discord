<template>
  <div class="sidebar">
    <img alt="EURE Logo" src="../assets/eure.png">
    <button type="button" @click="toggleActivity">Toggle Activity</button>
  </div>
  <div class="container">
    <div class="leaderboard">
      <transition-group name="lb" class="table" tag="table">
        <tr v-for="(t, pos) in leaderboard" :key="t.teamName" class="team-content">
          <td><h2 class="rank">{{ pos + 1 }}</h2></td>

          <td>
            <div class="team">
              <p class="name">{{ t.teamName }}</p>
              <div class="stat">
                <div class="icon">
                  <svg viewBox="0 0 512 512">
                    <path
                        d="M494.579, 8.034c-6.065-5.52-15.512-4.987-21.311, 0.812l-4.591, 4.591L458.383, 3.144c-3.825-3.824-10.05-4.278-14.113-0.708 c-4.452, 3.911-4.616, 10.699-0.493, 14.822l10.539, 10.539l-17.189, 17.189l-6.107-6.107c-5.948-5.949-15.592-5.949-21.54, 0 L300.816, 147.543c-4.309, 4.309-5.479, 10.552-3.546, 15.929l-10.829, 10.829c-1.667-0.613-3.438-0.951-5.248-0.951 c-4.039, 0-7.914, 1.605-10.77, 4.461l-17.05, 17.05l-16.066-16.066c-8.804-8.805-22.721-9.513-32.372-1.648l-62.144, 50.63 c-5.294, 4.313-8.529, 10.698-8.876, 17.518c-0.347, 6.819, 2.223, 13.5, 7.051, 18.328l21.823, 21.823l-17.05, 17.05 c-2.856, 2.856-4.461, 6.731-4.461, 10.77c0, 1.844, 0.35, 3.647, 0.984, 5.34l-10.54, 10.414c-5.378-1.933-11.621-0.763-15.929, 3.546 L16.889, 431.47c-6.053, 6.053-5.931, 15.904, 0.271, 21.805l57.312, 54.528c6.708, 6.382, 17.51, 5.353, 22.892-2.181l81.439-113.996 c4.147-6.049, 3.394-14.196-1.793-19.382l-8.297-8.297l8.46-8.477l13.393, 13.393l25.486, 75.927 c2.127, 6.336, 8.041, 10.387, 14.433, 10.387c1.324, 0, 2.669-0.174, 4.006-0.537l65.393-17.794c4.953-1.348, 9.089-4.95, 10.751-9.806 c2.363-6.906-0.44-14.297-6.435-17.972l-6.313-3.871l-19.653-72.227l17.782-15.091c26.909, 19.082, 59.273, 29.52, 92.555, 29.52 c5.66, 0, 11.349-0.302, 17.04-0.914c5.617-0.604, 10.41-4.341, 12.46-9.604l17.355-44.554c1.93-4.955, 1.136-10.557-2.094-14.781 s-8.427-6.459-13.717-5.892c-25.788, 2.753-51.469-2.374-73.869-14.126c0.105-1.014, 0.188-2.031, 0.23-3.055 c0.565-13.828-4.724-27.372-14.51-37.158l-8.06-8.06l8.79-8.79l15.078, 15.078c2.865, 2.865, 6.743, 4.461, 10.769, 4.461 c0.316, 0, 0.633-0.01, 0.95-0.03c4.362-0.272, 8.396-2.405, 11.077-5.856l95.034-122.295c4.712-6.064, 4.173-14.686-1.257-20.116 l-5.181-5.181l36.445-36.445C501.232, 23.961, 501.054, 13.927, 494.579, 8.034z M226.933, 309.254l-7.802, 7.801 c-1.983, 1.982-4.581, 2.974-7.18, 2.974s-5.198-0.991-7.18-2.974c-3.965-3.965-3.965-10.395, 0.001-14.36l7.802-7.801 c3.966-3.965, 10.396-3.965, 14.36, 0C230.899, 298.859, 230.899, 305.289, 226.933, 309.254z M184.329, 263.906l-16.905-16.905 l52.354-42.654l12.055, 12.055L184.329, 263.906z"></path>
                  </svg>
                </div>
                <p>{{ t.aks }} AKs</p>
              </div>
              <div class="stat">
                <div class="icon">
                  <svg viewBox="0 0 512 512">
                    <path
                        d="m256 71c-90.980469 0-165 74.019531-165 165 0 54.136719 26.011719 103.96875 70 134.917969v45.082031c0 8.285156 6.714844 15 15 15h35v-45c0-8.285156 6.714844-15 15-15s15 6.714844 15 15v45h30v-45c0-8.285156 6.714844-15 15-15s15 6.714844 15 15v45h35c8.285156 0 15-6.714844 15-15v-45.082031c43.988281-30.949219 70-80.78125 70-134.917969 0-90.980469-74.019531-165-165-165zm-60 201c-24.8125 0-45-20.1875-45-45s20.1875-45 45-45 45 20.1875 45 45-20.1875 45-45 45zm120 0c-24.8125 0-45-20.1875-45-45s20.1875-45 45-45 45 20.1875 45 45-20.1875 45-45 45zm0 0"/>
                    <path
                        d="m131 416v-30.316406c-8.546875-7.136719-16.46875-14.988282-23.667969-23.4375l-40.375 40.375c-7.117187-6.535156-16.53125-10.621094-26.957031-10.621094-22.09375 0-40 17.90625-40 40s17.90625 40 40 40c0 22.09375 17.90625 40 40 40 22.089844 0 40-17.90625 40-40 0-10.425781-4.085938-19.835938-10.621094-26.957031l22.148438-22.148438c-.347656-2.25-.527344-4.550781-.527344-6.894531zm0 0"/>
                    <path
                        d="m421.472656 132.949219 23.570313-23.570313c7.117187 6.535156 16.53125 10.621094 26.957031 10.621094 22.09375 0 40-17.910156 40-40 0-22.09375-17.90625-40-40-40 0-22.09375-17.90625-40-40-40s-40 17.90625-40 40c0 10.425781 4.085938 19.835938 10.621094 26.957031l-20.472656 20.472657c15.328124 13.035156 28.617187 28.390624 39.324218 45.519531zm0 0"/>
                    <path
                        d="m472 392c-10.425781 0-19.839844 4.085938-26.957031 10.621094l-40.375-40.375c-7.199219 8.449218-15.121094 16.300781-23.667969 23.4375v30.316406c0 2.34375-.179688 4.644531-.527344 6.894531l22.148438 22.148438c-6.535156 7.121093-10.621094 16.53125-10.621094 26.957031 0 22.09375 17.910156 40 40 40 22.09375 0 40-17.90625 40-40 22.09375 0 40-17.90625 40-40s-17.90625-40-40-40zm0 0"/>
                    <path
                        d="m40 120c10.425781 0 19.839844-4.085938 26.957031-10.621094l23.570313 23.570313c10.707031-17.128907 23.996094-32.484375 39.324218-45.519531l-20.472656-20.472657c6.535156-7.121093 10.621094-16.53125 10.621094-26.957031 0-22.09375-17.90625-40-40-40s-40 17.90625-40 40c-22.09375 0-40 17.90625-40 40 0 22.089844 17.90625 40 40 40zm0 0"/>
                  </svg>
                </div>
                <p>{{ t.kills }} Kills</p>
              </div>
            </div>
          </td>
          <td class="desktop">
            <div class="stat">
              <div class="icon">
                <svg viewBox="0 0 512 512">
                  <path
                      d="M494.579, 8.034c-6.065-5.52-15.512-4.987-21.311, 0.812l-4.591, 4.591L458.383, 3.144c-3.825-3.824-10.05-4.278-14.113-0.708 c-4.452, 3.911-4.616, 10.699-0.493, 14.822l10.539, 10.539l-17.189, 17.189l-6.107-6.107c-5.948-5.949-15.592-5.949-21.54, 0 L300.816, 147.543c-4.309, 4.309-5.479, 10.552-3.546, 15.929l-10.829, 10.829c-1.667-0.613-3.438-0.951-5.248-0.951 c-4.039, 0-7.914, 1.605-10.77, 4.461l-17.05, 17.05l-16.066-16.066c-8.804-8.805-22.721-9.513-32.372-1.648l-62.144, 50.63 c-5.294, 4.313-8.529, 10.698-8.876, 17.518c-0.347, 6.819, 2.223, 13.5, 7.051, 18.328l21.823, 21.823l-17.05, 17.05 c-2.856, 2.856-4.461, 6.731-4.461, 10.77c0, 1.844, 0.35, 3.647, 0.984, 5.34l-10.54, 10.414c-5.378-1.933-11.621-0.763-15.929, 3.546 L16.889, 431.47c-6.053, 6.053-5.931, 15.904, 0.271, 21.805l57.312, 54.528c6.708, 6.382, 17.51, 5.353, 22.892-2.181l81.439-113.996 c4.147-6.049, 3.394-14.196-1.793-19.382l-8.297-8.297l8.46-8.477l13.393, 13.393l25.486, 75.927 c2.127, 6.336, 8.041, 10.387, 14.433, 10.387c1.324, 0, 2.669-0.174, 4.006-0.537l65.393-17.794c4.953-1.348, 9.089-4.95, 10.751-9.806 c2.363-6.906-0.44-14.297-6.435-17.972l-6.313-3.871l-19.653-72.227l17.782-15.091c26.909, 19.082, 59.273, 29.52, 92.555, 29.52 c5.66, 0, 11.349-0.302, 17.04-0.914c5.617-0.604, 10.41-4.341, 12.46-9.604l17.355-44.554c1.93-4.955, 1.136-10.557-2.094-14.781 s-8.427-6.459-13.717-5.892c-25.788, 2.753-51.469-2.374-73.869-14.126c0.105-1.014, 0.188-2.031, 0.23-3.055 c0.565-13.828-4.724-27.372-14.51-37.158l-8.06-8.06l8.79-8.79l15.078, 15.078c2.865, 2.865, 6.743, 4.461, 10.769, 4.461 c0.316, 0, 0.633-0.01, 0.95-0.03c4.362-0.272, 8.396-2.405, 11.077-5.856l95.034-122.295c4.712-6.064, 4.173-14.686-1.257-20.116 l-5.181-5.181l36.445-36.445C501.232, 23.961, 501.054, 13.927, 494.579, 8.034z M226.933, 309.254l-7.802, 7.801 c-1.983, 1.982-4.581, 2.974-7.18, 2.974s-5.198-0.991-7.18-2.974c-3.965-3.965-3.965-10.395, 0.001-14.36l7.802-7.801 c3.966-3.965, 10.396-3.965, 14.36, 0C230.899, 298.859, 230.899, 305.289, 226.933, 309.254z M184.329, 263.906l-16.905-16.905 l52.354-42.654l12.055, 12.055L184.329, 263.906z"></path>
                </svg>
              </div>
              <p>{{ t.aks }} AKs</p>
            </div>
          </td>
          <td class="desktop">
            <div class="stat">
              <div class="icon">
                <svg viewBox="0 0 512 512">
                  <path
                      d="m256 71c-90.980469 0-165 74.019531-165 165 0 54.136719 26.011719 103.96875 70 134.917969v45.082031c0 8.285156 6.714844 15 15 15h35v-45c0-8.285156 6.714844-15 15-15s15 6.714844 15 15v45h30v-45c0-8.285156 6.714844-15 15-15s15 6.714844 15 15v45h35c8.285156 0 15-6.714844 15-15v-45.082031c43.988281-30.949219 70-80.78125 70-134.917969 0-90.980469-74.019531-165-165-165zm-60 201c-24.8125 0-45-20.1875-45-45s20.1875-45 45-45 45 20.1875 45 45-20.1875 45-45 45zm120 0c-24.8125 0-45-20.1875-45-45s20.1875-45 45-45 45 20.1875 45 45-20.1875 45-45 45zm0 0"/>
                  <path
                      d="m131 416v-30.316406c-8.546875-7.136719-16.46875-14.988282-23.667969-23.4375l-40.375 40.375c-7.117187-6.535156-16.53125-10.621094-26.957031-10.621094-22.09375 0-40 17.90625-40 40s17.90625 40 40 40c0 22.09375 17.90625 40 40 40 22.089844 0 40-17.90625 40-40 0-10.425781-4.085938-19.835938-10.621094-26.957031l22.148438-22.148438c-.347656-2.25-.527344-4.550781-.527344-6.894531zm0 0"/>
                  <path
                      d="m421.472656 132.949219 23.570313-23.570313c7.117187 6.535156 16.53125 10.621094 26.957031 10.621094 22.09375 0 40-17.910156 40-40 0-22.09375-17.90625-40-40-40 0-22.09375-17.90625-40-40-40s-40 17.90625-40 40c0 10.425781 4.085938 19.835938 10.621094 26.957031l-20.472656 20.472657c15.328124 13.035156 28.617187 28.390624 39.324218 45.519531zm0 0"/>
                  <path
                      d="m472 392c-10.425781 0-19.839844 4.085938-26.957031 10.621094l-40.375-40.375c-7.199219 8.449218-15.121094 16.300781-23.667969 23.4375v30.316406c0 2.34375-.179688 4.644531-.527344 6.894531l22.148438 22.148438c-6.535156 7.121093-10.621094 16.53125-10.621094 26.957031 0 22.09375 17.910156 40 40 40 22.09375 0 40-17.90625 40-40 22.09375 0 40-17.90625 40-40s-17.90625-40-40-40zm0 0"/>
                  <path
                      d="m40 120c10.425781 0 19.839844-4.085938 26.957031-10.621094l23.570313 23.570313c10.707031-17.128907 23.996094-32.484375 39.324218-45.519531l-20.472656-20.472657c6.535156-7.121093 10.621094-16.53125 10.621094-26.957031 0-22.09375-17.90625-40-40-40s-40 17.90625-40 40c-22.09375 0-40 17.90625-40 40 0 22.089844 17.90625 40 40 40zm0 0"/>
                </svg>
              </div>
              <p>{{ t.kills }} Kills</p>
            </div>
          </td>
        </tr>
      </transition-group>
    </div>
    <div v-if="!hideActivity" class="feed">
      <h2>Recent Activity</h2>
      <ul>
        <li v-for="msg in activityFeed" :key="msg.id">
          {{ msg.msg }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script lang="ts">
import {computed, defineComponent, ref, toRefs} from "vue"

class Team {
  teamName: string
  kills: number
  aks: number

  constructor(teamName: string, kills: number, aks: number) {
    this.teamName = teamName
    this.kills = kills
    this.aks = aks
  }

  get score() {
    return this.aks * .3 + this.kills
  }
}

type Activity = {
  id: number
  msg: string
}

export default defineComponent({
  props: {
    eventSource: {
      type: EventSource,
      required: true
    }
  },
  setup(props) {
    const teams = ref(new Map<string, Team>())
    const activity = ref<Activity[]>([])
    const leaderboard = computed(() => [...teams.value.values()].sort((a, b) => a.score > b.score ? -1 : 1))
    const activityFeed = computed(() => activity.value.slice().reverse())
    const hideActivity = ref((sessionStorage.getItem("hideActivity") ?? 'true').toLowerCase() === "true")

    const {eventSource} = toRefs(props)
    let lastId = 0

    eventSource.value.onmessage = (event: MessageEvent) => {
      const [method, ...data] = event.data.split("\n")
      switch (method) {
        case 'team': {
          teams.value.set(data[0], new Team(data[0], parseInt(data[1]), parseInt(data[2])))
          break
        }
        case 'gun': {
          const n = parseInt(data[1]) - teams.value.get(data[0])!.aks
          teams.value.get(data[0])!.aks = parseInt(data[1])
          if (n > 0) {
            const match = activity.value.length ? activity.value[activity.value.length - 1].msg.match(`${data[0]} deposited (\\d+) AKs`) : null
            if (match != null) {
              activity.value[activity.value.length - 1].msg = `${data[0]} deposited ${n + Number.parseInt(match[1])} AKs`
            } else {
              activity.value.push({
                id: lastId++,
                msg: `${data[0]} deposited ${n} AKs`
              })
            }
          }
          break
        }
        case 'kill': {
          teams.value.get(data[0])!.kills = parseInt(data[1])
          activity.value.push({
            id: lastId++,
            msg: `${data[2]} killed ${data[3]}`
          })
          break
        }
      }
    }

    function toggleActivity() {
      hideActivity.value = !hideActivity.value
      sessionStorage.setItem("hideActivity", hideActivity.value.toString())
    }

    return {
      teams,
      activity,
      leaderboard,
      activityFeed,
      hideActivity,
      toggleActivity,
    }
  }
})

</script>

<style lang="scss">
.container {
  width: 100%;
  display: flex;
  flex-wrap: wrap;

  @media screen and (min-width: 980px) {
    flex-wrap: nowrap;
  }
}

.sidebar {
  padding: 1rem 2rem;
  border: none;
  border-bottom: #9c9c9c solid 2px;
  margin: 0;
  position: sticky;
  top: 0;
  left: 0;
  display: flex;
  background: #242627;
  z-index: 1;
  flex: 0 0 100%;
  max-width: none;
  height: 80px;
  flex-direction: row;

  img {
    height: 100%;
  }

  button {
    border: none;
    padding: .4em;
    border-radius: 4px;
    text-transform: uppercase;
    color: #000;
    background-color: #ffcc00;
    font-size: .9rem;
    font-weight: 500;
    outline: none;
    cursor: pointer;
    margin-left: auto;
    touch-action: manipulation;

    &:hover {
      box-shadow: rgba(0, 0, 0, 0.15) 2px 5px;
    }
  }

  @media screen and (min-width: 980px) {
    flex: 0 0 20vw;
    height: 100vh;
    flex-direction: column;
    max-width: 260px;
    border-right: #9c9c9c solid 2px;
    background: transparent;
    z-index: 0;

    img {
      padding-bottom: 2rem;
      width: 100%;
      height: auto;
    }

    button {
      margin: 0;
    }
  }
}

.leaderboard {
  width: 100%;
  margin: 0 auto;
  padding: 1rem 2rem;
  max-width: 1200px;
}

.lb-move {
  transition: transform .8s ease;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 1.2em;

  h2 {
    text-align: center;
    padding-right: 1rem;
  }

  tr:nth-child(-n + 3) > td p:first-child {
    font-weight: 800;
  }

  tr {
    border-bottom: .1em solid #ffffff;
    height: 80px;

    &:last-child {
      border: none;
    }
  }

  td:nth-child(2) {
    width: 100%;
  }

  .desktop {
    display: none;
  }

  @media screen and (min-width: 980px) {
    .team > .stat {
      display: none;
    }
    .desktop {
      display: table-cell;
    }
  }
}

.feed {
  width: 100%;
  font-size: 1.2em;
  overflow-y: scroll;
  padding: 2rem 2rem 1rem 2rem;
  margin: 0 auto;
  height: 15em;
  order: -1;

  @media screen and (min-width: 1500px) {
    width: 30%;
  }

  @media screen and (min-width: 980px) {
    width: 40%;
    order: 1;
    height: 100vh;
    position: sticky;
    top: 0;
    right: 0;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }

  ul {
    padding: 0;
    margin: 0;
    list-style: none;
    color: #9c9c9c;
  }
}

.name {
  display: block;
}

.stat {
  padding: 0 1rem 0 0;
  display: inline-block;
  white-space: nowrap;
}

p {
  display: inline-block;
}

.icon {
  display: inline-flex;
  align-self: center;
  margin-right: .2rem;
  padding: 0;
}

svg {
  height: 1em;
  width: 1em;
  position: relative;
  top: .125em;
  fill: #fff;
}
</style>
