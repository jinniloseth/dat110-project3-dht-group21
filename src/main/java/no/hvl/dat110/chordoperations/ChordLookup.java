/**
 * 
 */
package no.hvl.dat110.chordoperations;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.middleware.Message;
import no.hvl.dat110.middleware.Node;
import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class ChordLookup {

	private static final Logger logger = LogManager.getLogger(ChordLookup.class);
	private Node node;

	public ChordLookup(Node node) {
		this.node = node;
	}

	/**
	 * This method finds the successor of the key by asking the node to find it.
	 * 
	 * @param key BigInteger
	 * @return NodeInterface (successor node)
	 * @throws RemoteException
	 */
	public NodeInterface findSuccessor(BigInteger key) throws RemoteException {
		NodeInterface successor = node.getSuccessor();

	    // Fallback dersom successor er null (skal egt ikke skje)
	    if (successor == null || successor.getNodeID() == null) {
	        return node; // returner deg selv som beste gjetning
	    }

	    // Sjekker om nøkkelen er i intervallet (nodeID, successorID]
	    if (Util.checkInterval(key, node.getNodeID(), successor.getNodeID())) {
	        return successor;
	    }

	   
	    NodeInterface highestPred = findHighestPredecessor(key);
	    if (highestPred.equals(node)) {
	        return node; // unngå uendelig løkke hvis du er nærmest
	    }

	    return highestPred.findSuccessor(key);
	}
	/**
	 * This method finds the highest predecessor to a given key. It checks the
	 * finger table entries for the closest node to the key.
	 * 
	 * @param key BigInteger
	 * @return NodeInterface (closest predecessor node)
	 * @throws RemoteException
	 */
	private NodeInterface findHighestPredecessor(BigInteger key) throws RemoteException {
		// Collect the entries in the finger table for this node
		List<NodeInterface> fingerTable = node.getFingerTable();

		// Iterate over the finger table starting from the last entry
		for (int i = fingerTable.size() - 1; i >= 0; i--) {
			NodeInterface fingerNode = fingerTable.get(i);
			BigInteger fingerID = fingerNode.getNodeID();

			// Check if the finger node is between (nodeID + 1, key - 1) using the
			// checkInterval method
			if (Util.checkInterval(fingerID, node.getNodeID(), key)) {
				return fingerNode; // Return the finger node as it is the closest to the key
			}
		}

		// If no finger found, return this node as it is the closest
		return node;
	}

	public void copyKeysFromSuccessor(NodeInterface succ) {
		Set<BigInteger> filekeys;
		try {
			// if this node and succ are the same, don't do anything
			if(succ.getNodeName().equals(node.getNodeName()))
				return;
			
			logger.info("copy file keys that are <= "+node.getNodeName()+" from successor "+ succ.getNodeName()+" to "+node.getNodeName());
			
			filekeys = new HashSet<>(succ.getNodeKeys());
			BigInteger nodeID = node.getNodeID();
			
			for(BigInteger fileID : filekeys) {

				if(fileID.compareTo(nodeID) <= 0) {
					logger.info("fileID="+fileID+" | nodeID= "+nodeID);
					node.addKey(fileID); 															
					Message msg = succ.getFilesMetadata().get(fileID);				
					node.saveFileContent(msg.getNameOfFile(), fileID, msg.getBytesOfFile(), msg.isPrimaryServer()); 			
					succ.removeKey(fileID); 	 																				
					succ.getFilesMetadata().remove(fileID); 																	
				}
			}
			
			logger.info("Finished copying file keys from successor "+ succ.getNodeName()+" to "+node.getNodeName());
		} catch (RemoteException e) {
			logger.error(e.getMessage());
		}
	}
	public void notify(NodeInterface pred_new) throws RemoteException {
		NodeInterface pred_old = node.getPredecessor();

		// if the predecessor is null accept the new predecessor
		if (pred_old == null) {
			node.setPredecessor(pred_new); // accept the new predecessor
			return;
		}

		else if (pred_new.getNodeName().equals(node.getNodeName())) {
			node.setPredecessor(null);
			return;
		} else {
			BigInteger nodeID = node.getNodeID();
			BigInteger pred_oldID = pred_old.getNodeID();

			BigInteger pred_newID = pred_new.getNodeID();

			// check that pred_new is between pred_old and this node, accept pred_new as the
			// new predecessor
			// check that ftsuccID is a member of the set {nodeID+1,...,ID-1}
			boolean cond = Util.checkInterval(pred_newID, pred_oldID.add(BigInteger.ONE), nodeID.add(BigInteger.ONE));
			if (cond) {
				node.setPredecessor(pred_new); // accept the new predecessor
			}
		}
		// Similar logic as before, to handle notifications from the new predecessor
		// node
	}

}
