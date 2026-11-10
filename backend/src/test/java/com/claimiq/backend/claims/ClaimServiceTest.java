package com.claimiq.backend.claims;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
class ClaimServiceTest {
 ClaimRepository repository=mock(ClaimRepository.class);
 ClaimService service=new ClaimService(repository,new ClaimMapper());
 ClaimRequest request=new ClaimRequest("PAT-100","PROV-100",new BigDecimal("125.00"),"USD",LocalDate.of(2026,10,1));
 @BeforeEach void setup(){when(repository.save(any(Claim.class))).thenAnswer(i->i.getArgument(0));}
 @Test void createsDraft(){ClaimResponse response=service.create(request);assertEquals(ClaimStatus.DRAFT,response.status());assertTrue(response.claimNumber().startsWith("CLM-"));verify(repository).save(any());}
 @Test void submitsDraft(){UUID id=UUID.randomUUID();Claim claim=new Claim();when(repository.findById(id)).thenReturn(Optional.of(claim));ClaimResponse result=service.submit(id);assertEquals(ClaimStatus.SUBMITTED,result.status());assertNotNull(result.submittedAt());}
 @Test void rejectsApprovalBeforeReview(){UUID id=UUID.randomUUID();Claim claim=new Claim();when(repository.findById(id)).thenReturn(Optional.of(claim));assertThrows(InvalidClaimTransitionException.class,()->service.transition(id,ClaimStatus.APPROVED));}
 @Test void missingClaim(){UUID id=UUID.randomUUID();when(repository.findById(id)).thenReturn(Optional.empty());assertThrows(ClaimNotFoundException.class,()->service.get(id));}
 @Test void preventsEditingSubmittedClaim(){UUID id=UUID.randomUUID();Claim claim=new Claim();claim.setStatus(ClaimStatus.SUBMITTED);when(repository.findById(id)).thenReturn(Optional.of(claim));assertThrows(InvalidClaimTransitionException.class,()->service.update(id,request));}
}