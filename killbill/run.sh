# Create paypal_express.yml file if test.mode = false

if [ "${TEST_MODE}" == "false" ]; then
  var1=":paypal_express:"
  var2="  :test: false"
  logwrite="$var1\n$var2"
  echo -e "$logwrite"  >> /var/tmp/bundles/plugins/ruby/killbill-paypal-express/6.0.0/paypal_express.yml
fi

# Run catalina.sh
sh /killbill/bin/catalina.sh run