<template>
  <div class="page-container">
    <md-app md-waterfall>
      <md-app-toolbar class="md-primary">
        <md-button class="md-icon-button" @click="menuVisible = !menuVisible">
          <md-icon>menu</md-icon>
        </md-button>
        <span class="md-title">Analytic State Store Viewer</span>
      </md-app-toolbar>
      <md-app-drawer :md-active.sync="menuVisible">
        <md-button @click="navigate('tumbling')">
          <md-icon>line_style</md-icon>
          Tumbling
        </md-button>
        <br/>
        <md-button @click="navigate('hopping')">
          <md-icon>line_style</md-icon>
          Hopping
        </md-button>
        <br/>
        <md-button @click="navigate('sliding')">
          <md-icon>line_style</md-icon>
          Sliding
        </md-button>
        <br/>
        <md-button @click="navigate('session')">
          <md-icon>line_style</md-icon>
          Session
        </md-button>
        <br/>
        <md-button @click="navigate('no-windowing')">
          <md-icon>line_style</md-icon>
          No Windowing
        </md-button>
      </md-app-drawer>
      <md-app-content>
        <router-view></router-view>
      </md-app-content>
    </md-app>
  </div>
</template>
<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.1s;
}

.fade-enter,
.fade-leave-to
  /* .fade-leave-active in <2.1.8 */
{
  opacity: 0;
}

.md-app {
  /*max-height: 600px;*/
  border: 1px solid rgba(#000, .12);
}

.md-app-drawer {
  width: 130px;
  /*max-width: calc(100vw - 125px);*/
}

.md-app-toolbar {
  /*height: 196px;*/
}
</style>
<script>
export default {
  data() {
    return {
      menuVisible: false,
    }
  },
  methods: {
    navigate(location) {
      this.menuVisible = false;
      console.log("location: " + location)
      this.$router.replace({name: location}).catch(() => {
      });
    }
  },
  created() {
  }
};
</script>
