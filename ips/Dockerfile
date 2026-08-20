FROM alpine:3.23

RUN apk add --no-cache bash iptables snort

RUN wget -qO /tmp/snort3-community-rules.tar.gz \
      https://www.snort.org/downloads/community/snort3-community-rules.tar.gz \
    && tar -xzf /tmp/snort3-community-rules.tar.gz -C /etc/snort/ \
    && rm /tmp/snort3-community-rules.tar.gz

COPY snort.lua /etc/snort/snort.lua
COPY local.rules /etc/snort/local.rules

COPY --chmod=0755 entrypoint.sh /
COPY --chmod=0755 snort.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD ["/snort.sh"]
