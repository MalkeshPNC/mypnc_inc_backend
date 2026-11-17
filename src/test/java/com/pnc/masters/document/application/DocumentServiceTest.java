package com.pnc.masters.document.application;

import com.pnc.masters.customer.Customer;
import com.pnc.masters.customer.CustomerRepository;
import com.pnc.masters.customer.api.CustomerNotFoundException;
import com.pnc.masters.document.Document;
import com.pnc.masters.document.DocumentProperties;
import com.pnc.masters.document.DocumentRepository;
import com.pnc.masters.document.DocumentStorage;
import com.pnc.masters.document.api.DocumentValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;
    @Mock
    private DocumentStorage storage;
    @Mock
    private CustomerRepository customerRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(repository, storage, new DocumentProperties(), customerRepository);
    }

    @Test
    void uploadStoresLibraryFileNamedWithCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", "data".getBytes());
        when(storage.store(any(), any(InputStream.class)))
                .thenReturn(new DocumentStorage.StoredDocument(4, "checksum"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = documentService.upload("Contract", null, file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storage).store(keyCaptor.capture(), any(InputStream.class));
        assertThat(keyCaptor.getValue()).matches("library/contract/library_contract_[0-9a-f]{8}_scan.pdf");
        assertThat(response.customerId()).isNull();
        assertThat(response.category()).isEqualTo("Contract");
        assertThat(response.originalFilename()).isEqualTo("scan.pdf");
        assertThat(response.storedFilename()).startsWith("library_contract_");
    }

    @Test
    void uploadStoresCustomerFileNamedWithCustomerAndCategory() throws Exception {
        Customer customer = new Customer();
        customer.setCustId(42L);
        when(customerRepository.findByCustIdAndIsDeletedFalse(42L)).thenReturn(Optional.of(customer));
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "img".getBytes());
        when(storage.store(any(), any(InputStream.class)))
                .thenReturn(new DocumentStorage.StoredDocument(3, "checksum"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.upload("Company logo", 42L, file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storage).store(keyCaptor.capture(), any(InputStream.class));
        assertThat(keyCaptor.getValue()).matches("42/company-logo/42_company-logo_[0-9a-f]{8}_logo.png");
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(repository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getCustomerId()).isEqualTo(42L);
    }

    @Test
    void uploadRejectsBlankCategory() {
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", "data".getBytes());
        assertThatThrownBy(() -> documentService.upload("  ", null, file))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessage("category is required");
    }

    @Test
    void uploadRejectsUnknownCustomer() {
        when(customerRepository.findByCustIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "scan.pdf", "application/pdf", "data".getBytes());
        assertThatThrownBy(() -> documentService.upload("Contract", 99L, file))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void buildStorageKeySanitizesCategoryAndOriginalName() {
        String key = DocumentService.buildStorageKey(7L, "ID proof", "abc-def-12345678", "../secret.pdf");
        assertThat(key).isEqualTo("7/id-proof/7_id-proof_abcdef12_secret.pdf");
    }
}
