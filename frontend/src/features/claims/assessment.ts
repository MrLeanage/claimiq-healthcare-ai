export interface Assessment {
  claimId: string;
  claimVersion: number;
  assessment: {
    modelVersion: string;
    trainingData: 'SYNTHETIC';
    assessedAt: string;
    score: number;
    riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
    interceptLogOdds: number;
    findings: {
      code: string;
      title: string;
      field: string;
      evidence: string;
      contributionLogOdds: number;
      suggestedCorrection: string;
    }[];
    limitations: string[];
  };
}
