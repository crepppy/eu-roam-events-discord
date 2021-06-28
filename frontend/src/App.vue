<template>
  <EventLeaderboard v-if="event" :event-source="eventSource"/>
  <svg v-else-if="loading" class="loading" viewBox="0 0 100 100" x="0px" xml:space="preserve" y="0px">
    <path d="M73,50c0-12.7-10.3-23-23-23S27,37.3,27,50 M30.9,50c0-10.5,8.5-19.1,19.1-19.1S69.1,39.5,69.1,50">
      <animateTransform
          attributeName="transform"
          attributeType="XML"
          dur="1s"
          from="0 50 50"
          repeatCount="indefinite"
          to="360 50 50"
          type="rotate"/>
    </path>
  </svg>
  <template v-else>
    <video id="bgvideo" autoplay loop muted playsinline poster="./assets/blur.jpg">
      <source src="./assets/blur.webm" type="video/webm">
      <source src="./assets/blur.mp4" type="video/mp4">
    </video>
    <div class="overlay">
      <div class="info">
        <h1>No event currently running!</h1>
        <p>We run events every Sunday at 7pm. Join our discord to be notified of event signups</p>
      </div>
    </div>
  </template>
</template>

<script lang="ts">
import EventLeaderboard from "@/components/EventLeaderboard.vue"
import {ref} from "vue"

export default {
  components: {EventLeaderboard},
  setup() {
    const eventSource = ref(new EventSource("/api/game"))
    let loading = ref(true)
    let event = ref(false) 

    eventSource.value.onerror = () => {
      loading.value = false
    }

    eventSource.value.onopen = () => {
      loading.value = false
      event.value = true
    }

    return {
      eventSource,
      loading,
      event,
    }
  }
}
</script>

<style lang="scss">
#app {
  font-family: 'Roboto', sans-serif;
  color: white;
  background-color: #242627;
  display: flex;
  min-height: 100vh;
  flex-wrap: wrap;

  @media screen and (min-width: 980px) {
    flex-wrap: nowrap;
  }
}

.info {
  margin: auto;
  padding: 0 1rem 5rem 1rem;
  color: #fff;
  font-size: 1.3em;
}

.overlay {
  position: relative;
  height: 100vh;
  width: 100vw;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

#bgvideo {
  object-fit: cover;
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
}

.loading {
  width: 100px;
  height: 100px;
  margin: auto;
  display: inline-block;
  visibility: hidden;
  fill: #fff;
  animation: 0ms linear 300ms forwards loadingWait;

  @keyframes loadingWait {
    to {
      visibility: visible;
    }
  }
}
</style>