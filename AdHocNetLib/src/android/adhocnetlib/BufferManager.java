package android.adhocnetlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

public class BufferManager {
	
	public static class Data implements Serializable {
		private static final long serialVersionUID = 1L;
		public byte[] bytes = null;
		public long time = 0;		
		public Data(byte[] b, long t) {
			bytes = b;
			time = t;
		}
	}
	
	public static class BufferItem implements Serializable {
		private static final long serialVersionUID = 1L;
		public Data payload = null;
		long ttl = 0;
		/* nodeIDs is a list of destinations that should ultimately 
		 * have the BufferItem stored when communication takes place
		 */
		HashSet<UUID> nodeIDs = new HashSet<UUID>();
	
		public BufferItem (byte[] bytes, long ttl, UUID nodeID) {
			payload = new Data(bytes, new Date().getTime());
			this.ttl = ttl;
			nodeIDs.add(nodeID);
		}
		
		public static void serialize (BufferItem item, OutputStream out) 
		throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(item);
		}
		
		public static BufferItem deserialize (InputStream in) 
		throws StreamCorruptedException, IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(in);
			BufferItem item = (BufferItem) ois.readObject();
			return item;
		}
		
		public int hashCode() {
			return payload.hashCode();
		}
	}
	
	private Hashtable<Integer, BufferItem> bufferedItems = new Hashtable<Integer, BufferItem>();
	
	public BufferManager() {
		/* Do nothing by default */
	}
	
	public synchronized BufferItem addNewItem(byte[] bytes, long ttl, UUID nodeID) {
		return _addItem(new BufferItem(bytes, ttl, nodeID));		
	}
	
	public synchronized BufferItem addItem (BufferItem item) {
		return _addItem(item);
	}
		
	public synchronized ArrayList<BufferItem> addItems (Collection<BufferItem> items) {
		ArrayList<BufferItem> newItems = new ArrayList<BufferItem>();		
		for (BufferItem item: items) {
			if (_addItem(item) != null) {
				newItems.add(item);
			}
		}		
		return newItems;
	}
	
	public synchronized boolean addNodeIDToItems (Collection<BufferItem> items, UUID nodeID) {
		for (BufferItem item: items) {
			BufferItem bufferedItem = getDuplicateItem(item);
			if (bufferedItem == null) return false;
			bufferedItem.nodeIDs.add(nodeID);
		}
		return true;
	}
	
	public synchronized ArrayList<BufferItem> getAllItemsForNodeID  (UUID nodeID) {
		ArrayList<BufferItem> filteredItems = new ArrayList<BufferItem>();
		for (BufferItem bufferedItem : bufferedItems.values()) {
			if (nodeID == null || !bufferedItem.nodeIDs.contains(nodeID)) {
				filteredItems.add(bufferedItem);
			}
		}		
		return filteredItems;
	}
	
	public synchronized Collection<BufferItem> getAllItems () {
		return bufferedItems.values();
	}

	public int getBufferSize() {
		return bufferedItems.size();
	}
	
	private BufferItem _addItem (BufferItem item) {
		BufferItem alreadyExisting = getDuplicateItem(item);
		if (alreadyExisting != null) {
			alreadyExisting.nodeIDs.addAll(item.nodeIDs);
			return null;
		} else {
			bufferedItems.put(item.hashCode(), item);
			return item;
		}	
	}
	
	private void removeExpiredItems() {
		
	}
	
	private boolean isDuplicate (Data data) {
		if (data != null && bufferedItems.contains(new Integer(data.hashCode()))) {
			return true;
		}
		return false;
	}
	
	private boolean isDuplicate (BufferItem item) {
		return bufferedItems.contains(item.hashCode());
	}
	
	private BufferItem getDuplicateItem (BufferItem item) {
		return (BufferItem) bufferedItems.get(item.hashCode());
	}
}
