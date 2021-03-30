/*
 * Copyright 2021 Haulmont.
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

package io.jmix.reports.app;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

public class GsonEntityBuilder {

    protected final static String METADATA_STARTS_CHAR = "_";

    public enum SerializationPolicy {
        ignoreMetadata,
        ignoreFields,
        convertAll
    }

    public Gson build() {
        return new GsonBuilder()
                .create();
    }

    public Gson build(SerializationPolicy serializationPolicy, String... ignoreFields) {
        if (serializationPolicy.equals(SerializationPolicy.ignoreMetadata)) {
            return buildIgnoreMetadata();
        } else if (serializationPolicy.equals(SerializationPolicy.ignoreFields)) {
            return buildIgnoreFields(ignoreFields);
        } else {
            return build();
        }
    }

    protected Gson buildIgnoreMetadata() {
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getName().startsWith(METADATA_STARTS_CHAR);
            }
        };

        return new GsonBuilder()
                .addSerializationExclusionStrategy(strategy)
                .create();
    }

    protected Gson buildIgnoreFields(String... ignoreFields) {
        if (ArrayUtils.isNotEmpty(ignoreFields)) {
            ExclusionStrategy strategy = new ExclusionStrategy() {
                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }

                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    return Arrays
                            .stream(ignoreFields)
                            .anyMatch(fieldName -> field.getName().equals(fieldName));
                }
            };

            return new GsonBuilder()
                    .addSerializationExclusionStrategy(strategy)
                    .create();
        } else {
            return buildIgnoreMetadata();
        }
    }
}
