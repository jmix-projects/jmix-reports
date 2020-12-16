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

package io.jmix.reportsui.gui.definition.edit.crosstab;

import com.google.common.base.Strings;
import io.jmix.core.Metadata;
import io.jmix.reports.entity.BandDefinition;
import io.jmix.reports.entity.DataSet;
import io.jmix.reports.entity.Orientation;
import io.jmix.reports.entity.Report;
import io.jmix.reports.util.DataSetFactory;
import io.jmix.reportsui.gui.definition.edit.BandDefinitionEditor;
import io.jmix.security.constraint.PolicyStore;
import io.jmix.security.constraint.SecureOperations;
import io.jmix.ui.UiComponents;
import io.jmix.ui.component.Table;
import io.jmix.ui.component.TextField;
import io.jmix.ui.model.CollectionContainer;
import io.jmix.ui.model.InstanceContainer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Class presents decorator been for add some extra behavior on report band orientation change
 *
 * @see BandDefinitionEditor#initDataSetListeners()
 */
public class CrossTabTableDecorator {

    protected static final String HORIZONTAL_TPL = "dynamic_header";
    protected static final String VERTICAL_TPL = "master_data";

    @Autowired
    protected DataSetFactory dataSetFactory;

    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    protected SecureOperations secureOperations;

    @Autowired
    protected PolicyStore policyStore;

    @Autowired
    protected Metadata metadata;

    public void decorate(Table<DataSet> dataSets, final InstanceContainer<BandDefinition> bandDefinitionDs) {
        dataSets.addGeneratedColumn("name", entity -> {
            TextField textField = uiComponents.create(TextField.class);
            textField.setParent(dataSets);
            textField.setWidthFull();
            textField.setHeightAuto();
            textField.setValue(entity.getName());
            //TODO item datasource
//            textField.setDatasource(dataSets.getItemDatasource(entity), "name");

            if (bandDefinitionDs.getItem() != null) {
                if (Orientation.CROSS == bandDefinitionDs.getItem().getOrientation() &&
                        !Strings.isNullOrEmpty(entity.getName()) &&
                        (entity.getName().endsWith(HORIZONTAL_TPL) || entity.getName().endsWith(VERTICAL_TPL))) {
                    textField.setEditable(false);
                }
            }
            textField.setEditable(isUpdatePermitted());
            return textField;
        });

        //todo
//        bandDefinitionDs.addItemChangeListener(band -> {
//            if (VALID == dataSets.getDatasource().getState()) {
//                onTableReady(dataSets, bandDefinitionDs);
//            } else {
//                dataSets.getDatasource().addStateChangeListener(new Datasource.StateChangeListener<DataSet>() {
//                    @Override
//                    public void stateChanged(Datasource.StateChangeEvent<DataSet> e) {
//                        if (VALID == e.getState()) {
//                            onTableReady(dataSets, bandDefinitionDs);
//                            dataSets.getDatasource().removeStateChangeListener(this);
//                        }
//                    }
//                });
//            }
//        });
    }

    protected boolean isUpdatePermitted() {
        return secureOperations.isEntityUpdatePermitted(metadata.getClass(Report.class), policyStore);
    }

    protected void onHorizontalSetChange(DataSet dataSet) {
        dataSet.setName(String.format("%s_" + HORIZONTAL_TPL, dataSet.getBandDefinition().getName()));
    }

    protected void onVerticalSetChange(DataSet dataSet) {
        dataSet.setName(String.format("%s_" + VERTICAL_TPL, dataSet.getBandDefinition().getName()));
    }


    protected void onTableReady(Table<DataSet> dataSets, InstanceContainer<BandDefinition> bandDefinitionDs) {
        //todo
//        CollectionDatasource<DataSet, UUID> dataSetsDs = dataSets.getDatasource();
//
//        initCrossDatasets(dataSetsDs, bandDefinitionDs);
    }

    protected void initCrossDatasets(CollectionContainer<DataSet> dataSetsDs,
                                     InstanceContainer<BandDefinition> bandDefinitionDs) {
        if (bandDefinitionDs.getItem() == null) {
            return;
        }

        DataSet horizontal = null;
        DataSet vertical = null;

        for (DataSet dataSet : dataSetsDs.getItems()) {
            if (horizontal == null && !Strings.isNullOrEmpty(dataSet.getName()) && dataSet.getName().endsWith(HORIZONTAL_TPL)) {
                horizontal = dataSet;
            }

            if (vertical == null && !Strings.isNullOrEmpty(dataSet.getName()) && dataSet.getName().endsWith(VERTICAL_TPL)) {
                vertical = dataSet;
            }

            if (horizontal != null && vertical != null) break;
        }

        if (horizontal == null) {
            horizontal = dataSetFactory.createEmptyDataSet(bandDefinitionDs.getItem());
            onHorizontalSetChange(horizontal);
        }

        if (vertical == null) {
            vertical = dataSetFactory.createEmptyDataSet(bandDefinitionDs.getItem());
            onVerticalSetChange(vertical);
        }

        initListeners(dataSetsDs, bandDefinitionDs, horizontal, vertical);
    }

    protected void initListeners(CollectionContainer<DataSet> dataSetsDs,
                                 InstanceContainer<BandDefinition> bandDefinitionDs,
                                 DataSet horizontal, DataSet vertical) {
        bandDefinitionDs.addItemPropertyChangeListener(e -> {
            if ("orientation".equals(e.getProperty())) {
                Orientation orientation = (Orientation) e.getValue();
                Orientation prevOrientation = (Orientation) e.getPrevValue();
                if (orientation == prevOrientation) return;

                if (Orientation.CROSS == orientation || Orientation.CROSS == prevOrientation) {
                    onOrientationChange(dataSetsDs, bandDefinitionDs);
                }

                if (Orientation.CROSS == orientation) {
                    dataSetsDs.getItems().add(horizontal);
                    dataSetsDs.getItems().add(vertical);
                }
            }

            if (bandDefinitionDs.getItem().getOrientation() == Orientation.CROSS && "name".equals(e.getProperty())) {
                onHorizontalSetChange(horizontal);
                onVerticalSetChange(vertical);
            }
        });
    }

    protected void onOrientationChange(CollectionContainer<DataSet> dataSetsDs, InstanceContainer<BandDefinition> bandDefinitionDs) {
        //todo
        //        dataSetsDs.getItems().stream()
//                .filter(Objects::nonNull)
//                .forEach(dataSetsDs::removeItem);
        dataSetsDs.getItems().add(dataSetFactory.createEmptyDataSet(bandDefinitionDs.getItem()));
    }
}
