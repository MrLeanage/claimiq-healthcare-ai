package com.claimiq.backend.claims;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class ClaimService {
 private final ClaimRepository repository; private final ClaimMapper mapper;
 public ClaimService(ClaimRepository repository,ClaimMapper mapper){this.repository=repository;this.mapper=mapper;}
 public ClaimResponse create(ClaimRequest request){
  Claim claim=new Claim();mapper.apply(request,claim);
  claim.setClaimNumber("CLM-"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase());
  return mapper.toResponse(repository.save(claim));
 }
 @Transactional(readOnly=true) public ClaimResponse get(UUID id){return mapper.toResponse(find(id));}
 @Transactional(readOnly=true) public Page<ClaimResponse> list(Pageable pageable){return repository.findAll(pageable).map(mapper::toResponse);}
 public ClaimResponse update(UUID id,ClaimRequest request){Claim claim=find(id);require(claim.getStatus()==ClaimStatus.DRAFT,"Only draft claims can be edited");mapper.apply(request,claim);return mapper.toResponse(repository.save(claim));}
 public ClaimResponse submit(UUID id){Claim claim=find(id);require(claim.getStatus()==ClaimStatus.DRAFT,"Only draft claims can be submitted");claim.setStatus(ClaimStatus.SUBMITTED);claim.setSubmittedAt(Instant.now());return mapper.toResponse(repository.save(claim));}
 public ClaimResponse transition(UUID id,ClaimStatus target){
  Claim claim=find(id);ClaimStatus current=claim.getStatus();
  boolean valid=(current==ClaimStatus.SUBMITTED && target==ClaimStatus.UNDER_REVIEW) || (current==ClaimStatus.UNDER_REVIEW && (target==ClaimStatus.APPROVED || target==ClaimStatus.REJECTED));
  require(valid,"Invalid transition from "+current+" to "+target);
  claim.setStatus(target);return mapper.toResponse(repository.save(claim));
 }
 private Claim find(UUID id){return repository.findById(id).orElseThrow(()->new ClaimNotFoundException(id));}
 private void require(boolean ok,String message){if(!ok)throw new InvalidClaimTransitionException(message);}
}