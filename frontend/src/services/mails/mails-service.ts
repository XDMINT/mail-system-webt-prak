import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { API_BASE_URL } from '../../constants';
import { CreateMail, Mail, MailListItem, PageResponse, UpdateMail } from '../../types/mails';
import { User } from '../../types/user';

@Injectable({
  providedIn: 'root',
})
export class MailsService {
  private http = inject(HttpClient);

  public getIncomingMails(page = 0, size = 25) {
    return this.http.get<PageResponse<MailListItem>>(`${API_BASE_URL}/mails/incoming`, {
      params: {
        page,
        size,
      },
    });
  }

  public getDrafts() {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/drafts`);
  }

  public getSentMails() {
    return this.http.get<Mail[]>(`${API_BASE_URL}/mails/sent`);
  }

  public getMailById(id: string) {
    return this.http.get<Mail>(`${API_BASE_URL}/mails/${id}`);
  }

  public sendMail(id: string) {
    return this.http.post<Mail>(`${API_BASE_URL}/mails/send/${id}`, {});
  }

  public createReplyDraft(id: string) {
    return this.http.post<Mail>(`${API_BASE_URL}/mails/${id}/reply`, {});
  }

  public deleteMail(id: string) {
    return this.http.delete(`${API_BASE_URL}/mails/${id}`);
  }

  public getAllUsers() {
    return this.http.get<User[]>(`${API_BASE_URL}/users`);
  }

  public ensureUser(email: string) {
    return this.http.post<User>(`${API_BASE_URL}/users/ensure`, { email });
  }

  public createDraft(mail: CreateMail, files: File[]) {
    const formData = this.createFormData(mail, files);
    return this.http.post<Mail>(`${API_BASE_URL}/mails`, formData);
  }

  public createAndSendMail(mail: CreateMail, files: File[]) {
    const formData = this.createFormData(mail, files);
    return this.http.post<Mail>(`${API_BASE_URL}/mails/send`, formData);
  }

  public updateMails(id: string, mail: UpdateMail, files: File[]) {
    const formData = this.createFormData(mail, files);
    return this.http.put<Mail>(`${API_BASE_URL}/mails/${id}`, formData);
  }

  public fetchAttachment(id: string) {
    return this.http.get(`${API_BASE_URL}/attachments/${id}`, { responseType: 'blob' });
  }

  private createFormData(mail: CreateMail | UpdateMail, files: File[]): FormData {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(mail)], { type: 'application/json' }));
    if (files.length === 0) {
      formData.append('attachments', new Blob([], { type: 'application/octet-stream' }));
    } else {
      files.forEach((file) => {
        formData.append('attachments', file);
      });
    }
    return formData;
  }
}
