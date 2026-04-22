export type CashflowDirection = 'GIVE' | 'TAKE';

export type CashflowItem = {
  id: number;
  personId: number;
  categoryId: number;
  price: number;
  item: string;
  direction: CashflowDirection;
  inflationRate: number;
  changedPrice: number;
  date: string; // YYYY-MM-DD
  createdAt: string;
  updatedAt: string;
};

export type CashflowCategoryTotal = {
  categoryId: number;
  give: number;
  take: number;
  net: number;
};

export type CashflowResponse = {
  status: string;
  data: {
    history: CashflowItem[];
    categoryTotals: CashflowCategoryTotal[];
    totalGive: number;
    totalTake: number;
    net: number;
  };
  message?: string;
};
