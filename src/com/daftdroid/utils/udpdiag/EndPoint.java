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
	public static void main (String args[])
	{
		String addr = args[0];
		int port = Integer.parseInt(args[1]);

		if (addr.toLowerCase().equals("server"))
			server = true;

		tx = new TxThread();
		rx = server ? new RxThread(port) : new RxThread(addr, port);
		
		tx.start();
		// tx waits on keyboard input. If we are the client, it sends it to the known server
		// address, if we are the server, we assume the first kb input occurs after the client
		// has contacted us so we know its address.
		
		rx.start();
		
	}
	static boolean server;
	
	static Object sync = new Object(); // allows the two following items to be modified atomically
	static InetAddress lastSenderAddress;
	static int lastSenderPort;
	static int listenPort; // if we are server
	static String remoteServerAddr; // if we are client
	static int remoteServerPort; // if we are client
	static int ephemeralPort; // if we are the client
	static TxThread tx;
	static RxThread rx;
	
	static class RxThread extends Thread
	{
	
		public RxThread(int port)
		{
			listenPort = port;
			remoteServerAddr = null;
			remoteServerPort = 0;
		}
		public RxThread(String addr, int port)
		{
			remoteServerAddr = addr;
			remoteServerPort = port;
			listenPort = 0;
		}
		
		@Override
		public void run()
		{
			/*	The receive thread on the client should wait until something is transmitted */
			if (!server)
			{
				synchronized(this)
				{
					while (ephemeralPort == 0)
						try {wait();}catch (InterruptedException e) {return;}
				}
			}
			try
			(
				DatagramSocket rxSocket = new DatagramSocket(listenPort);
			)
			{
				
				byte[] rxBuf = new byte[65536];
				DatagramPacket rxPacket = new DatagramPacket(rxBuf, rxBuf.length);
				
				while (true)
				{
					rxSocket.receive(rxPacket);
					
					InetAddress senderAddr = rxPacket.getAddress();
					int senderPort = rxPacket.getPort();
					
					if (server)
					{
						/*
						 * Note the address of the current "client"
						 */
						synchronized (sync)
						{
							lastSenderAddress = senderAddr;
							lastSenderPort = senderPort;
						}
					}
					
					byte[] rxData = Arrays.copyOf(rxBuf, rxPacket.getLength());
					String s = new String(rxData);

					System.out.println("["+senderAddr.getHostAddress()+":"+senderPort+"]:"+s);
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
		
			
			try (DatagramSocket sock = new DatagramSocket();)
			{
				
				while (true)
				{
					InetSocketAddress toAddress = null;
					String txt = buffer.readLine();
					
					
					if (server)
					{
						synchronized(sync)
						{
							if (lastSenderAddress == null)
							{
								System.out.println("No client to reply to!");
								continue;
							}
							toAddress = new InetSocketAddress(lastSenderAddress, lastSenderPort);
						}
					}
					else if (toAddress == null)
					{
						synchronized (sync)
						{
							toAddress = new InetSocketAddress(remoteServerAddr, remoteServerPort);
						}
					}


					byte[] hello = txt.getBytes();
					DatagramPacket p = new DatagramPacket(hello, hello.length, toAddress);
					sock.send(p);
					
					if (!server && ephemeralPort == 0)
					{
						synchronized(rx)
						{
							ephemeralPort = sock.getLocalPort();
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
