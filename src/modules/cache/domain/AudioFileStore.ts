export interface AudioFileStore {
  exists: (filePath: string) => Promise<boolean>;
  delete: (filePath: string) => Promise<void>;
}
