package com.trinusphere.beebiteifood.domain.credential.repository;

import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IfoodCredentialRepository {

    IfoodCredential save(IfoodCredential credential);

    Optional<IfoodCredential> findByStoreId(UUID storeId);

    Optional<IfoodCredential> findByCatalogId(UUID catalogId);

    Optional<IfoodCredential> findByMerchantId(String merchantId);

    List<IfoodCredential> findAllEnabled();

    void deleteByStoreId(UUID storeId);
}
