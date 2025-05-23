/**
 * 
 */
package no.hvl.dat110.chordoperations;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Set;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.middleware.Node;
import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.Hash;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class ChordProtocols {
	
	private static final Logger logger = LogManager.getLogger(ChordProtocols.class);
	/**
	 * UpdateSuccessor
	 * StabilizeRing
	 * FixFingerTable
	 * CheckPredecessor
	 */
	
	private NodeInterface chordnode;
	private StabilizationProtocols stabprotocol;
	
	public ChordProtocols(NodeInterface chordnode) {
		this.chordnode = chordnode;
		joinRing();
		stabilizationProtocols();
	}
	
	/**
	 * Public access bcos it's used by the GUI
	 */
	public void stabilizationProtocols() {
		Timer timer = new Timer();
		stabprotocol = new StabilizationProtocols(this, timer);
		timer.scheduleAtFixedRate(stabprotocol, 5000, 2000);
	}
	
	/**
	 * Public access bcos it's used by the GUI
	 */
	public void joinRing() {
		
		//Registry registry = Util.tryIPs();											// try the trackers IP addresses
		try {
			Registry registry = Util.tryIPSingleMachine(chordnode.getNodeName());
 			// try the ports
			if(registry != null) {
				try {
					String foundNode = Util.activeIP;
	
					NodeInterface randomNode = (NodeInterface) registry.lookup(foundNode);
					
					logger.info("JoinRing-randomNode = "+randomNode.getNodeName());
					
					// call remote findSuccessor function. The result will be the successor of this randomNode
					NodeInterface chordnodeSuccessor = randomNode.findSuccessor(chordnode.getNodeID());
	
					// set the successor of this node to chordnodeSuccessor and its predecessor to null
					chordnode.setSuccessor(chordnodeSuccessor);	
					chordnode.setPredecessor(null);
					
					// notify chordnodeSuccessor of a new predecessor
					chordnodeSuccessor.notify(chordnode);
					
					// fix the finger table - create a new routing table for this node
					fixFingerTable();
					
					// copy all keys that are less or equal (<=) to chordnode ID to chordnode
					((Node) chordnode).copyKeysFromSuccessor(chordnodeSuccessor);
					
					logger.info(chordnode.getNodeName()+" is between null | "+chordnodeSuccessor.getNodeName());
					
				} catch (Exception e) {
					logger.error("Feil i joinRing(): " + e.getMessage());
				}
			} else {
				
				createRing((Node) chordnode);		// no node is available, create a new ring
			}
		} catch (NumberFormatException | RemoteException e1) {
			logger.error("Feil under oppkobling mot registry: " + e1.getMessage());
			
		}
	}
	
	private void createRing(Node node) throws RemoteException {
		
		// set predecessor to nil - No predecessor for now
		node.setPredecessor(null);
		
		// set the successor to itself
		node.setSuccessor(node);
		
		logger.info("New ring created. Node = "+node.getNodeName()+" "
				+ "| Successor = "+node.getSuccessor().getNodeName()+
				  "| Predecessor = "+node.getPredecessor());
		
	}
	
	public void leaveRing() throws RemoteException {
		
		logger.info("Oppdaterer successor og predecessor før noden forlater ringen...");
		
		try {
		 
			NodeInterface prednode = chordnode.getPredecessor();														// get the predecessor			
			NodeInterface succnode = chordnode.getSuccessor();														// get the successor		
			Set<BigInteger> keyids = chordnode.getNodeKeys();									// get the keys for chordnode
			
			if(succnode != null) {												// add chordnode's keys to its successor
				keyids.forEach(fileID -> {
					try {
						logger.info("Adding fileID = "+fileID+" to "+succnode.getNodeName());

						succnode.addKey(fileID);
						Message msg = chordnode.getFilesMetadata().get(fileID);				
						succnode.saveFileContent(msg.getNameOfFile(), fileID, msg.getBytesOfFile(), msg.isPrimaryServer()); 			// save the file in memory of the newly joined node
					} catch (RemoteException e) {
						logger.info("Feil under nøkkeloverføring:" + e.getMessage());
					} 
				});

				succnode.setPredecessor(prednode); 							// set prednode as the predecessor of succnode
			}
			if(prednode != null) {
				prednode.setSuccessor(succnode);							// set succnode as the successor of prednode			
			} 
			chordnode.setSuccessor(chordnode);
			chordnode.setPredecessor(chordnode);
			chordnode.getNodeKeys().clear();
			stabprotocol.setStop(true);
			
		}catch(Exception e) {
			//
			logger.error("Feil under oppdatering av successor/predecessor/keys: " + e.getMessage());
		}
		
		logger.info("Oppdatering fullført. Noden har forlatt ringen.");
	}
	
	public void fixFingerTable() {
		
		try {
			logger.info("Oppdaterer FingerTable for Node: "+ chordnode.getNodeName());
	

			int m = Hash.bitSize();
			BigInteger addressSize = Hash.addressSize();
			
			chordnode.getFingerTable().clear();
			
			
			
	        for (int i = 0; i < m; i++) {
	        	BigInteger offset = BigInteger.valueOf(2).pow(i);
	        	BigInteger fingerID = chordnode.getNodeID().add(offset).mod(addressSize);
	            
	            logger.info("Beregner fingerID for finger " + i + ": " + fingerID);
	         
	            NodeInterface successor = chordnode.findSuccessor(fingerID);

	            if (successor != null) {
					chordnode.getFingerTable().add(successor);
					logger.info("Finger " + i + " peker nå til: " + successor.getNodeName());
				} else {
					logger.warn("Fant ingen etterfølger for fingerID: " + fingerID);
				}
			}
			
			// get the finger table from the chordnode (list object)
			
			// ensure to clear the current finger table
			
			// get the address size from the Hash class. This is the modulus and our address space (2^mbit = modulus)
			
			// get the number of bits from the Hash class. Number of bits = size of the finger table
			
			// iterate over the number of bits			
			
			// compute: k = succ(n + 2^(i)) mod 2^mbit
			
			// then: use chordnode to find the successor of k. (i.e., succnode = chordnode.findSuccessor(k))
			
			// check that succnode is not null, then add it to the finger table

		} catch (RemoteException e) {
	        // Håndter eventuelle RemoteExceptions som kan oppstå ved kommunikasjon
	        logger.error("Error while fixing finger table: " + e.getMessage(), e);
	    } catch (Exception e) {
	        // Håndter andre unntak som kan oppstå
	        logger.error("An unexpected error occurred while fixing the finger table: " + e.getMessage(), e);
	    }
	}

	protected NodeInterface getChordnode() {
		return chordnode;
	}

}
