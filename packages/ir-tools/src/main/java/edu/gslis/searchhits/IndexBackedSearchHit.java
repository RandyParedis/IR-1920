package edu.gslis.searchhits;

import java.lang.ref.SoftReference;

import edu.gslis.indexes.IndexWrapper;
import edu.gslis.textrepresentation.FeatureVector;

/**
 * A <code>SearchHit</code> object backed by an index.
 * 
 * <p>Allows you to request a <code>SearchHit</code>'s properties without
 * regard to whether those properties have already been fetched from the index.
 * Instances of <code>IndexBackedSearchHit</code> will return stored values
 * when available, otherwise it will look them up in the index as needed.
 * 
 * @author Garrick
 *
 */
public class IndexBackedSearchHit extends SearchHit {
	
	private IndexWrapper index;
	
	/**
	 * Using <code>SoftReference</code> to store the feature vector should help
	 * with garbage collection and memory usage issues.
	 */
	private SoftReference<FeatureVector> vector = new SoftReference<FeatureVector>(null);
	
	/**
	 * Creates a new <code>IndexBackedSearchHit</code> backed by the specified index
	 * and with values copied from another type of <code>SearchHit</code>
	 * 
	 * @param index The index backing this <code>SearchHit</code>
	 * @param toCopy The original <code>SearchHit</code> object to be copied
	 */
	public IndexBackedSearchHit(IndexWrapper index, SearchHit toCopy) {
		this(index);
		copyFromVanillaSearchHit(toCopy);
	}

	/**
	 * Creates a new <code>IndexBackedSearchHit</code> backed by the specified index
	 * @param index The index backing this <code>SearchHit</code>
	 */
	public IndexBackedSearchHit(IndexWrapper index) {
		super();
		setIndex(index);
	}
	
	public void setIndex(IndexWrapper index) {
		this.index = index;
	}
	
	public void copyFromVanillaSearchHit(SearchHit hit) {
		setDocno(hit.getDocno());
		setDocID(hit.getDocID());
		setFeatureVector(hit.getFeatureVector());
		setScore(hit.getScore());
		setLength(hit.getLength());
		setQueryName(hit.getQueryName());
	}
	
	@Override
	public int getDocID() {
		int docID = super.getDocID();
		if (docID == 0) {
			if (getDocno() == null) {
				System.err.println("SearchHit has neither docno nor docID. Giving docID of 0. This may crash your program.");
			} else {
				docID = index.getDocId(getDocno());
				setDocID(docID);
			}
		}
		return docID;
	}

	@Override
	public FeatureVector getFeatureVector() {
		FeatureVector vector = this.vector.get();
		if (vector == null) {
			if (getDocID() == 0) {
				System.err.println("SearchHit has no identification. Giving empty FeatureVector.");
				return new FeatureVector(null);
			} else {
				vector = index.getDocVector(getDocID(), null);
				setFeatureVector(vector);
				return vector;
			}
		}
		return vector;
	}
	
	@Override
	public void setFeatureVector(FeatureVector vector) {
		this.vector = new SoftReference<FeatureVector>(vector);
	}
}
