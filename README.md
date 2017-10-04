# UdpDiagnostics

Simple app for testing UDP.

Run the app at each end and type stuff into the terminal and see it appear at the other end.

This uses an application-level client-server model although UDP itself has no such construct.

**On the server**

    $ java cp . com.daftdroid.utils.udpdiag.EndPoint server 9999

**On the client**

(replace 9999 with the port number you want to listen on)

    $ java cp . com.daftdroid.utils.udpdiag.EndPoint 1.2.3.4 9999
    
(replace 1.2.3.4 and 9999 with the IP and port of the remote server)

Note: Once the server has received packets from a client it cannot be reached from other clients. This is becuase it uses `connect()`
which establishes a peer-to-peer relationship in spite of UDP being connectionless. As a simple diagnostic tool this does not
track sessions but I'm open to suggestion on changes to make it work with multiple clients.
