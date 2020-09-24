FROM itzg/minecraft-server

ENV EULA=true
ENV TYPE=PAPER
ENV VERSION=1.16.3
ENV CONSOLE=false
ENV TZ=Asia/Seoul

ENV INIT_MEMORY=256M
ENV MAX_MEMORY=1G
ENV JVM_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005

ENV ONLINE=true
ENV ENABLE_COMMAND_BLOCK=true
ENV MOTD="A Minecraft Debugging Server powered by Docker"

EXPOSE 5005

COPY docker-setup-plugin /

RUN dos2unix /docker-setup-plugin && chmod +x /docker-setup-plugin
