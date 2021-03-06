/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.pnc.rest.provider;

import com.google.common.base.Preconditions;
import org.jboss.pnc.datastore.limits.RSQLPageLimitAndSortingProducer;
import org.jboss.pnc.datastore.predicates.RSQLPredicate;
import org.jboss.pnc.datastore.predicates.RSQLPredicateProducer;
import org.jboss.pnc.datastore.repositories.LicenseRepository;
import org.jboss.pnc.model.License;
import org.jboss.pnc.rest.restmodel.LicenseRest;
import org.springframework.data.domain.Pageable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jboss.pnc.rest.utils.StreamHelper.nullableStreamOf;

@Stateless
public class LicenseProvider {

    private LicenseRepository licenseRepository;

    // needed for EJB/CDI
    public LicenseProvider() {
    }

    @Inject
    public LicenseProvider(LicenseRepository licenseRepository) {
        this.licenseRepository = licenseRepository;
    }

    public List<LicenseRest> getAll(int pageIndex, int pageSize, String sortingRsql, String query) {
        RSQLPredicate filteringCriteria = RSQLPredicateProducer.fromRSQL(License.class, query);
        Pageable paging = RSQLPageLimitAndSortingProducer.fromRSQL(pageSize, pageIndex, sortingRsql);

        return nullableStreamOf(licenseRepository.findAll(filteringCriteria.get(), paging))
                .map(toRestModel())
                .collect(Collectors.toList());
    }

    public LicenseRest getSpecific(Integer id) {
        License license = licenseRepository.findOne(id);
        if (license != null) {
            return new LicenseRest(license);
        }
        return null;
    }

    public Integer store(LicenseRest licenseRest) {
        Preconditions.checkArgument(licenseRest.getId() == null, "Id must be null");
        return licenseRepository.save(licenseRest.toLicense()).getId();
    }

    public void update(Integer id, LicenseRest licenseRest) {
        Preconditions.checkArgument(id != null, "Id must not be null");
        Preconditions.checkArgument(licenseRest.getId() == null || licenseRest.getId().equals(id),
                "Entity id does not match the id to update");
        licenseRest.setId(id);
        License license = licenseRepository.findOne(licenseRest.getId());
        Preconditions.checkArgument(license != null, "Couldn't find license with id " + licenseRest.getId());
        licenseRepository.saveAndFlush(licenseRest.toLicense());
    }

    public void delete(Integer id) {
        Preconditions.checkArgument(licenseRepository.exists(id), "Couldn't find license with id " + id);
        licenseRepository.delete(id);
    }

    public Function<? super License, ? extends LicenseRest> toRestModel() {
        return license -> new LicenseRest(license);
    }

}
