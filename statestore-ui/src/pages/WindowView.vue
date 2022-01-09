<template>
  <div id="app">
    <div class="md-layout md-gutter">
      <div class="md-layout-item">
        <md-field>
          <label>Analytics</label>
          <md-select v-model="windowType" name="windowType" id="windowType">
            <md-option value="tumbling">Tumbling</md-option>
            <md-option value="hopping">Hopping</md-option>
            <md-option value="sliding">Sliding</md-option>
            <md-option value="session">Session</md-option>
          </md-select>
        </md-field>
      </div>
      <div class="md-layout-item">
        <md-field>
          <label>Group Type</label>
          <md-select v-model="groupType" name="groupType" id="groupType">
            <md-option value="windowing">Windowing</md-option>
            <md-option value="sku">Sku</md-option>
          </md-select>
        </md-field>
      </div>
    </div>
    <vue-good-table
        ref="windowTable"
        :columns="columns"
        :rows="results"
        theme="black-rhino"
        :sort-options="{ enabled: true }"
        :group-options="{enabled: true, collapsable: true, headerPosition: 'top'}"
        :pagination-options="{
            enabled: false,
            //mode: 'records',
            //perPage: 100,
            //position: 'bottom',
            //perPageDropdown: [10, 50, 100],
            //dropdownAllowAll: false,
            // setCurrentPage: 1,
            // nextLabel: 'next',
            // prevLabel: 'prev',
            // rowsPerPageLabel: 'rows per page',
           //ofLabel: 'of',
            // pageLabel: 'page', // for 'pages' mode
            // allLabel: 'all'
          }">
      <template slot="table-header-row" slot-scope="props">
        <span style="font-weight: bold; color: blue;">{{ props.row.label }}</span>
      </template>
      <template slot="table-row" slot-scope="props">
            <span v-if="props.row.answered === props.row.questions">
              <span style="font-weight: bold; color: blue;">{{ props.formattedRow[props.column.field] }}</span>
            </span>
        <span v-else>
              {{ props.formattedRow[props.column.field] }}
            </span>
      </template>
    </vue-good-table>
    <md-progress-bar md-mode="indeterminate" v-if="sending"/>
  </div>
</template>
<script>

import api from '../plugins/api.js'
import {fromUnixTime, format} from 'date-fns'
import 'vue-good-table/dist/vue-good-table.css'
import {VueGoodTable} from 'vue-good-table';

export default {
  name: 'result',
  components: {
    VueGoodTable
  },
  props: {
    windowingType: false
  },
  watch: {
    windowType: function (newVal, oldVal) {
      this.load()
    },
    groupType: function (newVal, oldVal) {
      this.load()
    }
  },
  computed: {
    loadParams() {
      const params = new URLSearchParams();
      if (this.groupType != null) {
        params.append('group-type', this.groupType);
      }
      return params;
    }
  },
  methods: {
    load() {

      this.$refs.windowTable.collapseAll();

      this.sending = true
      this.results = null
      api.get("/" + this.windowType, {
        params: this.loadParams
      }).then(response => {
        window.console.log(response.data)
        if (response.status === 200) {
          this.results = response.data;
        } else {
        }
      }).catch((e) => {
        window.console.log(e)
      }).finally(() => {
            this.sending = false
          }
      )
    }
  },
  data() {
    return {
      sending: false,
      lastFetched: Date.now(),
      windowType: 'tumbling',
      groupType: 'windowing',
      results: [],
      columns: [
        {
          label: 'Sku',
          field: 'sku',
          filterable: true
        },
        {
          label: 'Timestamp',
          field: 'timestamp',
          filterable: false,
        },
        {
          label: 'Quantity',
          field: 'qty',
          filterable: true,
          type: 'number'
        },
        {
          label: 'Order Ids',
          field: 'orderIds',
          filterable: true
        },
      ]
    };
  },
  mounted() {
    this.load();
  }
}

</script>