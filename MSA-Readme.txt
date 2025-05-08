

.\bin\DirectSubscriber mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 btp_is_em solace-cloud-client r9ov0bcvdp9ge8q5bkv7v03tc1

.\bin\DirectPublisher mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 btp_is_em solace-cloud-client r9ov0bcvdp9ge8q5bkv7v03tc1


Common parameters:
         -h HOST[:PORT]  Appliance IP address [:port, omit for default]
         -u USER[@VPN]   Authentication username [@vpn, omit for default]
        [-w PASSWORD]    Authentication password
        [-z]             Enable compression
        [-x AUTH_METHOD] authentication scheme (One of : BASIC, KERBEROS). (Default: BASIC).  Specifying USER is mandatory when BASIC is used.

.\bin\featureQueueProvisionAndBrowse -h mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 -u solace-cloud-client@btp_is_em -w r9ov0bcvdp9ge8q5bkv7v03tc1 -x BASIC


QueueBrowser.java
MessageSelectorsOnQueue.java

.\bin\featureMessageSelectorsOnQueue.bat -h mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 -u solace-cloud-client@btp_is_em -w r9ov0bcvdp9ge8q5bkv7v03tc1 -x BASIC

-----------------------------

.\bin\CustomQueueBrowse -h mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 -u solace-cloud-client@btp_is_em -w r9ov0bcvdp9ge8q5bkv7v03tc1 -x BASIC -q Q/poc/input

.\bin\CustomMsgSelectorsOnQueue -h mr-connection-qzk92nm2y9z.messaging.solace.cloud:55555 -u solace-cloud-client@btp_is_em -w r9ov0bcvdp9ge8q5bkv7v03tc1 -x BASIC -q Q/poc/input


Message Id:                             73
Key 'SAP_MplCorrelationId' (String): AGgYp9ussDxRtrKhDPHZhIbMO_V3

Key 'pasta' (String): macaroni
