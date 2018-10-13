<template>
  <div class="text-xs-center">
    <v-dialog
      v-model="dialog"
      width="500"
    >

      <v-card>
        <v-card-title
          class="headline grey lighten-2"
          primary-title
        >
          {{title}}
        </v-card-title>

        <v-card-text>
          {{content}}
        </v-card-text>

        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            flat
            @click="agree"
          >
            {{okText}}
          </v-btn>
          <v-btn
            color="primary"
            flat
            @click="disAgree"
          >
            {{cancelText}}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';

  @Component
  export default class Confirm extends Vue {

    @Prop(String) title;
    @Prop(String) content;
    @Prop(String) okText;
    @Prop(String) cancelText;

    data() {
      return {
        dialog: false,
        callback: function () {
          return false;
        }
      }
    }

    open(callback) {
      this.dialog = true;
      this.callback = callback;
    }

    agree() {
      this.dialog = false;
      this.callback(true);
    }

    disAgree() {
      this.dialog = false;
      this.callback(false);
    }
  }
</script>
