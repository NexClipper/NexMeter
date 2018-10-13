<template>

  <v-layout row justify-center>
    <v-dialog v-model="dialog" fullscreen hide-overlay transition="dialog-bottom-transition">
      <v-card>
        <v-toolbar dark color="primary">
          <v-btn icon dark @click.native="dialog = false">
            <v-icon>close</v-icon>
          </v-btn>
          <v-toolbar-title>Unit Setting</v-toolbar-title>
          <v-spacer></v-spacer>
          <v-toolbar-items>
            <v-btn dark flat @click.native="save">Save</v-btn>
          </v-toolbar-items>
        </v-toolbar>

        <v-layout>
          <v-flex xs6>

            <v-subheader>General</v-subheader>

            <v-form v-model="valid" style="padding: 32px;">
              <v-text-field
                v-model="unit.name"
                label="Unit Name"
                clearable
                required
                :rules="[rules.required, rules.counter]"
                :counter="20"
              ></v-text-field>

              <br><br>

              <div class="caption">집계 방식은 생성 이후에는 변경 할 수 없습니다.</div>
              <v-select v-if="!id"
                        :items="['SUM', 'AVG', 'PEAK']"
                        label="Counting Method"
                        v-model="unit.countingMethod"
                        outline
              ></v-select>
              <v-text-field v-else
                            v-model="unit.countingMethod"
                            label="Counting Method"
                            readonly
                            outline
              ></v-text-field>

            </v-form>
          </v-flex>
          <v-flex xs6>
            <br>
            <catalog-viewer v-if="dialog"></catalog-viewer>
          </v-flex>
        </v-layout>
      </v-card>
    </v-dialog>
  </v-layout>
</template>

<script>
  import {Vue, Component, Prop, Watch} from 'vue-property-decorator';
  import CatalogViewer from '@/components/CatalogViewer'

  @Component({
    components: {
      'catalog-viewer': CatalogViewer
    }
  })
  export default class UnitEdit extends Vue {

    data() {
      return {
        dialog: false,
        notifications: false,
        sound: true,
        widgets: false,

        //unit id
        id: null,

        //init unit
        unit: this.emptyUnit(),

        //validator
        valid: false,
        rules: {
          required: value => !!value || 'Required.',
          counter: value => value && value.length <= 20 || 'Max 20 characters'
        }
      }
    }

    emptyUnit() {
      return {
        name: null,
        countingMethod: 'SUM',
        rules: []
      }
    }

    async open(id) {
      this.unit = this.emptyUnit();
      this.id = id;
      this.dialog = true;

      if (id) {
        var res = await axios.get('meter/units/' + id);
        this.unit = res.data;
      }
    }

    async save() {
      var me = this;
      if (!this.valid) {
        return;
      }
      try {
        //put
        if (this.id) {
          await axios.put('meter/units/' + this.id, this.unit);
        }
        //post
        else {
          await axios.post('meter/units', this.unit);
        }
        me.$root.$children[0].info('saved.');
        this.dialog = false;
        this.$emit('updated');
      } catch (e) {
        console.log(e.response);
        me.$root.$children[0].error('failed.');
      }
    }

    mounted() {

    }
  }
</script>
