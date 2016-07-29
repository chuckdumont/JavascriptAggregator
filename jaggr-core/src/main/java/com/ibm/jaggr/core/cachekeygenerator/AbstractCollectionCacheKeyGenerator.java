/*
 * (C) Copyright IBM Corp. 2012, 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jaggr.core.cachekeygenerator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract implementation of ICacheKeyGenerator that implements default logic for
 * key generators that utilize a collection of elements to define the key
 *
 * @param <T> the collection element type
 */
public abstract class AbstractCollectionCacheKeyGenerator<T> extends AbstractCacheKeyGenerator {
	private static final long serialVersionUID = 1523705011672523570L;

	protected abstract Collection<T> getCollection();

	/**
	 * Returns a new instance of this key generator. Subclasses override and implement.
	 *
	 * @param col
	 *            the collection of elements
	 * @param isProvisional
	 *            True if this key generator is provisional (i.e. is likely to change after
	 *            a module, or modules, that use this key generator has been built.
	 * @return the new instance
	 */
	protected abstract AbstractCollectionCacheKeyGenerator<T> newKeyGen(Collection<T> col, boolean isProvisional);

	/**
	 * Returns a cache key generator that is the combination of this cache key generator and the
	 * specified cache key generator (i.e. the cache keys generated by the returned object vary
	 * according to the conditions honored by this generator and the specified generator.
	 *
	 * @param otherKeyGen
	 *            the key generator to combine with this key generator
	 *
	 * @return the combined cache key generator. Can be this object or {@code otherKeyGen} if either
	 *         one already encompasses the other.
	 */
	public ICacheKeyGenerator combine(ICacheKeyGenerator otherKeyGen) {
		if (this.equals(otherKeyGen)) {
			return this;
		}
		@SuppressWarnings("unchecked")
		AbstractCollectionCacheKeyGenerator<T> other = (AbstractCollectionCacheKeyGenerator<T>)otherKeyGen;
		if (isProvisional() && other.isProvisional()) {
			// should never happen
			throw new IllegalStateException();
		}
		// If either generator is provisional, return a provisional result
		if (isProvisional()) {
			return other;
		} else if (other.isProvisional()) {
			return this;
		}
		if (getCollection() == null) {
			return this;
		}
		if (other.getCollection() == null) {
			return other;
		}
		// See if one of the keygens encompasses the other.  This is the most likely
		// case and is more performant than always creating a new keygen.
		int size = getCollection().size(), otherSize = other.getCollection().size();
		if (size > otherSize && getCollection().containsAll(other.getCollection())) {
			return this;
		}
		if (otherSize > size && other.getCollection().containsAll(getCollection())) {
			return other;
		}
		// Neither keygen encompasses the other, so create a new one that is a combination
		// of the both of them.
		Set<T> combined = new HashSet<T>();
		combined.addAll(getCollection());
		combined.addAll(other.getCollection());
		return newKeyGen(combined, false);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other != null && getClass().equals(other.getClass()) &&
				isProvisional() == ((AbstractCollectionCacheKeyGenerator<?>)other).isProvisional() &&
				(
						getCollection() != null && getCollection().equals(((AbstractCollectionCacheKeyGenerator<?>)other).getCollection()) ||
						getCollection() == null && ((AbstractCollectionCacheKeyGenerator<?>)other).getCollection() == null
				);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = getClass().hashCode();
		result = result * 31 + Boolean.valueOf(isProvisional()).hashCode();
		if (getCollection() != null) {
			result = result * 31 + getCollection().hashCode();
		}
		return result;
	}


}
