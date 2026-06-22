// 운영자별 처리 현황 API. apiClient(실 백엔드)를 통해 조회.
import { apiClient } from "../common/apiClient";

export interface OperatorStat {
  operatorId: string;
  username: string;
  role: string;
  assigned: number;
  approved: number;
  actions: number;
}

interface Envelope<T> {
  data: T;
}

export async function fetchOperatorStats(): Promise<OperatorStat[]> {
  const res = await apiClient.get<Envelope<OperatorStat[]>>("/dashboard/operator-stats");
  return res.data.data;
}
