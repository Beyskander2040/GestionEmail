export interface Page<T> {
    content: T[];
    pageable: any; // Replace with appropriate type if needed
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }