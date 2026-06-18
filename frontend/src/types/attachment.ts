export type Attachment = {
  id: string;
  size: number;
  fileName: string;
  mimeType: string;
  path: string;
  url: string;
  blob: Blob;
};
