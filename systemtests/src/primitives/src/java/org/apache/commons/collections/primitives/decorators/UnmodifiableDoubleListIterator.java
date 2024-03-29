/*
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.apache.commons.collections.primitives.decorators;

import org.apache.commons.collections.primitives.DoubleListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 1.2 $ $Date: 2004/02/25 20:46:27 $
 * 
 * @author Rodney Waldhoff 
 */
public final class UnmodifiableDoubleListIterator extends ProxyDoubleListIterator {
    UnmodifiableDoubleListIterator(DoubleListIterator iterator) {
        this.proxied = iterator;
    }
    
    public void remove() {
        throw new UnsupportedOperationException("This DoubleListIterator is not modifiable.");
    }

    public void add(double value) {
        throw new UnsupportedOperationException("This DoubleListIterator is not modifiable.");
    }

    public void set(double value) {
        throw new UnsupportedOperationException("This DoubleListIterator is not modifiable.");
    }

    protected DoubleListIterator getListIterator() {
        return proxied;   
    }
    
    
    public static final DoubleListIterator wrap(DoubleListIterator iterator) {
        if(null == iterator) {
            return null; 
        } else if(iterator instanceof UnmodifiableDoubleListIterator) {
            return iterator;
        } else {
            return new UnmodifiableDoubleListIterator(iterator);
        }
    }

    private DoubleListIterator proxied = null;    
}
