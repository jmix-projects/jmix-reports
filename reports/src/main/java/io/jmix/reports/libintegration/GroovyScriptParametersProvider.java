/*
 * Copyright (c) 2008-2019 Haulmont.
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

package io.jmix.reports.libintegration;

import com.haulmont.yarg.structure.BandData;
import com.haulmont.yarg.structure.ReportQuery;

import java.util.Map;


/**
 * Realization of this interface is intended to prepare a map of parameters filled with beans or other objects
 */

public interface GroovyScriptParametersProvider {
    /**
     * Prepares and return the map of objects
     * @param reportParameters - parameters to include into the map of parameters
     * @return map of objects
     */
    Map<String, Object> prepareParameters(ReportQuery reportQuery, BandData parentBand, Map<String, Object> reportParameters);
}
