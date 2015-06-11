/*
 * Copyright (c) 2015 Raytheon BBN Technologies Corp
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bbn.poi.xdgf.parsers.rx;

import com.bbn.poi.xdgf.parsers.ShapeData;
import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.geometry.Rectangle;

import rx.Subscriber;
import rx.functions.Action1;

// convenience functions
public class Rx {
	
	public static interface RTreeAction extends Action1<Entry<ShapeData, Rectangle>> {
		
	}
	
	public static abstract class RTreeSubscriber extends Subscriber<Entry<ShapeData, Rectangle>> {
		
		@Override
		public void onCompleted() {
			
		}

		@Override
		public void onError(Throwable e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}
