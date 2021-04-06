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

import com.haulmont.yarg.structure.ReportQuery;
import io.jmix.core.*;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.entity.DataSet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("StoreUtils")
public class StoreUtils {

    @Autowired
    private Metadata metadata;
    @Autowired
    private MetadataTools metadataTools;

    private StoreUtils() {
    }

    public String getStoreByQuery(ReportQuery reportQuery) {
        Map<String, Object> params = reportQuery.getAdditionalParams();
        if (params != null && params.get(DataSet.DATA_STORE_PARAM_NAME) != null) {
            return (String) params.get(DataSet.DATA_STORE_PARAM_NAME);
        } else {
            String query = reportQuery.getScript();
            return getStoreNameOrMain(query);
        }
    }

    protected String getStoreNameOrMain(String query) {
        String tableName = getTableByQuery(query);

        if(StringUtils.isNotBlank(tableName)){
            for (MetaClass namedMetaClass : metadata.getSession().getClasses()) {
                String sqlTableName = metadataTools.getDatabaseTable(namedMetaClass);

                if (StringUtils.isNotBlank(sqlTableName) && sqlTableName.equals(tableName)) {
                    return namedMetaClass.getStore().getName();
                }
            }
        }
        return Stores.MAIN;
    }


    protected String getTableByQuery(String queryString) {
        String ignoreOtherFrom = "from\\s+(?:\\w+\\.)*(\\w+)($|\\s+[WHERE,JOIN,START\\s+WITH,ORDER\\s+BY,GROUP\\s+BY])";
        Pattern p = Pattern.compile(ignoreOtherFrom, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(queryString);

        while (m.find()) {
            return m.group(1);
        }
        return StringUtils.EMPTY;
    }
}
