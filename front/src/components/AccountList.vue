<template>
  <v-card>
    <v-card-title>
      <v-layout row>
        Accounts
        <v-spacer></v-spacer>
        <v-text-field
          v-model="search"
          append-icon="search"
          label="Search"
          single-line
          hide-details
        ></v-text-field>
      </v-layout>
    </v-card-title>

    <v-data-table
      :pagination.sync="pagination"
      :headers="headers"
      :items="accounts"
      :total-items="totalAccounts"
      :rows-per-page-items="[5,10,15,25]"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <tr>
          <td @click="move(props.item.externalKey)" class="text-xs-right"><a>{{ props.item.externalKey}}</a></td>
          <td class="text-xs-right">{{ props.item.name }}</td>
          <td class="text-xs-right">{{ props.item.email }}</td>
          <td class="text-xs-right">{{ props.item.currency }}</td>
          <td class="text-xs-right">{{ props.item.country }}</td>
        </tr>
      </template>
      <template slot="no-data">
        <v-alert v-if="!loading" :value="true" color="error" icon="warning">
          Sorry, nothing to display here :(
        </v-alert>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';

  @Component
  export default class AccountList extends Vue {
    data() {
      return {
        totalAccounts: 0,
        pagination: {
          rowsPerPage: 10,
          page: 1
        },
        loading: true,
        accounts: [],
        search: '',
        headers: [
          {text: 'externalKey', value: 'externalKey'},
          {text: 'name', value: 'name'},
          {text: 'email', value: 'email'},
          {text: 'currency', value: 'currency'},
          {text: 'country', value: 'country'}
        ]
      }
    }

    mounted() {
      //this.load();
    }

    @Watch('pagination', {deep: true})
    @Watch('search')
    async load() {
      this.loading = true;
      var me = this;
      var res;
      var perPage = this.pagination.rowsPerPage;
      var page = this.pagination.page;
      var offset = (page - 1) * perPage;
      var limit = perPage;

      if (this.search && this.search.length > 0) {
        res = await axios.get('1.0/kb/accounts/search/'
          + encodeURIComponent(this.search) + '?offset=' + offset + '&limit=' + limit);
      } else {
        res = await axios.get('1.0/kb/accounts/pagination?offset=' + offset + '&limit=' + limit);
      }
      this.totalAccounts = parseInt(res.headers['x-killbill-pagination-totalnbrecords']);
      this.loading = false;
      this.accounts = res.data;
    }

    move(externalKey) {
      this.$router.push('/accounts/' + externalKey)
    }
  }
</script>
