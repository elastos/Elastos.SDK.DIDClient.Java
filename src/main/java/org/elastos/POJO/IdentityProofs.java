package com.github.fusionledger.elastos.sdk.did.pojo

import algebra.fields.AbstractFieldElementExpanded;
import algebra.curves.AbstractG1;
import algebra.curves.AbstractG2;
import algebra.msm.VariableBaseMSM;
import configuration.Configuration;
import reductions.r1cs_to_qap.R1CStoQAP;
import relations.objects.Assignment;
import relations.qap.QAPRelation;
import relations.qap.QAPWitness;
import scala.Tuple2;
import zk_proof_systems.zkSNARK.objects.Proof;
import zk_proof_systems.zkSNARK.objects.ProvingKey;


public class IdentityProofs {
    private KeyPair keyPair;
    private String address;
    private String proofType;
    

    public ProverKey getIdentityKey() {
        return IdentityKey;
    }

    public void setIdentityKey(ProvingKey IdentityKey) {
        this.ProvingKey = IdentityKey;
    }
  
   public String getProof() {
        return Proof;
    }
  
   public void setProof(proofType IdentityProof) {
        this.Proof = IdentityProof;
    }

}
