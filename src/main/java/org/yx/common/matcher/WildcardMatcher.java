/**
 * Copyright (C) 2016 - 2030 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.common.matcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class WildcardMatcher implements Predicate<String> {

	public static final String WILDCARD = "*";

	private final Set<String> exacts;

	private final String[] matchStarts;

	private final String[] matchEnds;

	private final String[] contains;

	WildcardMatcher(Set<String> exacts, String[] matchStarts, String[] matchEnds, String[] contains) {
		this.exacts = exacts;
		this.matchStarts = matchStarts;
		this.matchEnds = matchEnds;
		this.contains = contains;
	}

	@Override
	public boolean test(String text) {

		if (this.exacts != null && this.exacts.contains(text)) {

			return true;
		}

		if (this.matchStarts != null) {
			for (String start : this.matchStarts) {
				if (text.startsWith(start)) {

					return true;
				}
			}
		}

		if (this.matchEnds != null) {
			for (String end : this.matchEnds) {
				if (text.endsWith(end)) {

					return true;
				}
			}
		}

		if (this.contains != null) {
			for (String c : this.contains) {
				if (text.contains(c)) {
					return true;
				}
			}
		}
		return false;
	}

	public Collection<String> exacts() {
		return exacts == null ? null : Collections.unmodifiableSet(exacts);
	}

	public List<String> matchStarts() {
		return matchStarts == null ? null : Arrays.asList(matchStarts);
	}

	public List<String> matchEnds() {
		return matchEnds == null ? null : Arrays.asList(matchEnds);
	}

	public List<String> contains() {
		return contains == null ? null : Arrays.asList(contains);
	}

	@Override
	public String toString() {
		return "exacts=" + exacts + ", matchStarts=" + Arrays.toString(matchStarts) + ", matchEnds="
				+ Arrays.toString(matchEnds) + ", contains=" + Arrays.toString(this.contains);
	}

}
