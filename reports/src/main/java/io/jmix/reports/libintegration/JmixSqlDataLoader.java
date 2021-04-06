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

import com.haulmont.yarg.loaders.impl.SqlDataLoader;
import com.haulmont.yarg.structure.ReportQuery;
import com.haulmont.yarg.util.db.QueryRunner;
import com.haulmont.yarg.util.db.ResultSetHandler;
import io.jmix.data.StoreAwareLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Component("JmixSqlDataLoader")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class JmixSqlDataLoader extends SqlDataLoader {

    @Autowired
    protected List<DataSource> datasources;

    @Autowired
    protected StoreUtils storeUtils;

    @Autowired
    protected StoreAwareLocator storeAwareLocator;

    public JmixSqlDataLoader(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected List runQuery(ReportQuery reportQuery, String queryString, Object[] params, ResultSetHandler<List> handler) throws SQLException {
        if(datasources.size() > 1){
            String storeName = storeUtils.getStoreByQuery(reportQuery);

            QueryRunner runner = new QueryRunner(storeAwareLocator.getDataSource(storeName));
            return runner.query(queryString, params, handler);
        }
        return super.runQuery(reportQuery, queryString, params, handler);
    }


}

