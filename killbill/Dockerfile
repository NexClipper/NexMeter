FROM ubuntu:16.04
VOLUME /tmp

USER root

# Install Kill Bill dependencies and useful tools
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update && \
    apt-get install -y \
      apt-utils \
      curl \
      dpkg-dev \
      gcc \
      git \
      less \
      libapr1 \
      libapr1-dev \
      libssl-dev \
      make \
      mysql-client \
      net-tools \
      openjdk-8-jdk \
      python-pip \
      sudo \
      telnet \
      unzip \
      vim && \
    rm -rf /var/lib/apt/lists/*
ENV DEBIAN_FRONTEND teletype

# Configure default JAVA_HOME path
RUN ln -s java-8-openjdk-amd64 /usr/lib/jvm/default-java
ENV JAVA_HOME /usr/lib/jvm/default-java
ENV JSSE_HOME $JAVA_HOME/jre/

RUN apt-get update -y && apt-get install haveged procps vim wget -y
RUN update-rc.d haveged defaults

RUN mkdir -p /var/lib/jruby
RUN curl -SL http://jruby.org.s3.amazonaws.com/downloads/1.7.26/jruby-bin-1.7.26.tar.gz \
    | tar -z -x --strip-components=1 -C /var/lib/jruby

ENV PATH /var/lib/jruby/bin:$PATH
ENV JRUBY_OPTS=-J-Xmx1024m

# Install kpm
RUN gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3
RUN curl -sSL https://get.rvm.io | bash -s stable --ruby
RUN gem install kpm
RUN mkdir killbill && cd killbill && kpm install

ADD ./java_plugins.zip /java_plugins.zip
ADD ./ruby_plugins.zip /ruby_plugins.zip
RUN unzip /ruby_plugins.zip -d /var/tmp/bundles/plugins/ruby
RUN unzip /java_plugins.zip -d /var/tmp/bundles/plugins/java

ADD ./run.sh /killbill/bin/run.sh
ADD ./catalina.sh /killbill/bin/catalina.sh
ADD ./killbill-profiles-killbill-0.20.2.war /killbill/webapps/ROOT.war

RUN rm -rf /killbill/webapps/kaui* && rm -rf /killbill/webapps/ROOT
RUN chmod +x /killbill/webapps/ROOT.war
RUN chmod +x /killbill/bin/run.sh
RUN chmod +x /killbill/bin/catalina.sh

EXPOSE 8080 8001
CMD ["bash", "/killbill/bin/run.sh"]