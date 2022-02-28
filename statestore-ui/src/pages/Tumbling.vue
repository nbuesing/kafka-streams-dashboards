<template>
  <div class="wrapper">
    <span class="md-title">Tumbling Windows</span>
    <div class="main-panel">
      <window-view windowing-type="tumbling"></window-view>
    </div>
  </div>
</template>
<style lang="scss"></style>
<script>
import WindowView from "./WindowView.vue";

export default {
  components: {
    WindowView
  },
  data() {
    return {
    };
  },
  methods: {
  }
};
</script>
