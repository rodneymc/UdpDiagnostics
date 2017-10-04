package com.daftdroid.utils.udpdiag;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;


public class EndPoint
{
	public static void main (String args[]) throws Exception
	{
		String addr = args[0];
		int port = Integer.parseInt(args[1]);

		if (addr.toLowerCase().equals("server"))
			server = true;

		// Server: bind to the port
		if (server)
			socket = new DatagramSocket(port);
		else
		{
			// Client - local details are ephemeral
			socket = new DatagramSocket(); // totally ephemeral
			
			// Whereas remote details are known in advance
			socket.connect(new InetSocketAddress(addr, port));
		}
		
		
		
		tx = new TxThread();
		rx = new RxThread();
		
		tx.start();
		// tx waits on keyboard input. If we are the client, it sends it to the known server
		// address, if we are the server, we assume the first kb input occurs after the client
		// has contacted us so we know its address.
		
		rx.start();
		
	}
	static boolean server;
	
	static TxThread tx;
	static RxThread rx;
	static DatagramSocket socket;
	static boolean connected; // to make up for lack of udp state
	
	static class RxThread extends Thread
	{
		@Override
		public void run()
		{
			/*	The receive thread on the client should wait until something is transmitted */
			if (!server)
			{
				synchronized(this)
				{
					while (!connected)
						try {wait();}catch (InterruptedException e) {return;}
				}
			}
			try
			{
				
				byte[] rxBuf = new byte[65536];
				DatagramPacket rxPacket = new DatagramPacket(rxBuf, rxBuf.length);
				
				while (true)
				{
					socket.receive(rxPacket);
					
					InetAddress senderAddr = rxPacket.getAddress();
					int senderPort = rxPacket.getPort();
										
					byte[] rxData = Arrays.copyOf(rxBuf, rxPacket.getLength());
					String s = new String(rxData);

					System.out.println("["+senderAddr.getHostAddress()+":"+senderPort+"]:"+s);
					
					if (server && !connected)
					{
						synchronized(tx)
						{
							socket.connect(senderAddr, senderPort);
							connected = true;
							tx.notify();
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	static class TxThread extends Thread
	{
		
		@Override
		public void run()
		{
			BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
		
			try
			{
				if (server && !connected)
				{
					// Server must wait till a client "connects" before it can send anything
					synchronized(this)
					{
						while (!connected)
						{
							wait();
						}
					}
				}
				
				while (true)
				{
					String txt = buffer.readLine();
					
					byte[] hello = txt.getBytes();
					DatagramPacket p = new DatagramPacket(hello, hello.length);
					socket.send(p);
					
					if (!server && !connected)
					{
						synchronized(rx)
						{
							connected = true;
							rx.notify();
						}
					}
					
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	
}
