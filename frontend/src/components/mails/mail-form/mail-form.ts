import { Component, inject, signal, OnInit, Input, OnChanges } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormsModule,
} from '@angular/forms';
import { MailsService } from '../../../services/mails/mails-service';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Router } from '@angular/router';
import { Toast } from 'primeng/toast';
import { CommonModule } from '@angular/common';
import { MultiSelectModule } from 'primeng/multiselect';
import { TextareaModule } from 'primeng/textarea';
import { FileRemoveEvent, FileSelectEvent, FileUploadModule } from 'primeng/fileupload';
import { User } from '../../../types/user';
import { CreateMail, Mail, UpdateMail } from '../../../types/mails';
import { InputTextModule } from 'primeng/inputtext';
import { Attachment } from '../../../types/attachment';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ErrorResponse } from '../../../types/error';

@Component({
  selector: 'app-mail-form',
  standalone: true,
    imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ButtonModule,
    TextareaModule,
    FileUploadModule,
    Toast,
    InputTextModule,
    MultiSelectModule,
  ],
  templateUrl: './mail-form.html',
})
export class MailForm implements OnInit, OnChanges {
  @Input() mailData: Mail | null = null;
  @Input() title: string = 'Create Mail';

  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  protected mailForm = new FormGroup({
    subject: new FormControl('', [Validators.required, Validators.maxLength(255)]),
    content: new FormControl('', [Validators.required, Validators.maxLength(500)]),
  });

  protected availableUsers = signal<User[]>([]);
  // Now hold email addresses instead of user IDs (plain arrays for ngModel)
  protected selectedToUsers: string[] = [];
  protected selectedCcUsers: string[] = [];
  protected selectedBccUsers: string[] = [];

  // Input values for recipient fields (standalone from reactive form)
  protected toInputValue = '';
  protected ccInputValue = '';
  protected bccInputValue = '';

  // temporary input fields for fallback add (signals for proper change detection)
  protected uploadedFiles = signal<File[]>([]);
  protected isLoading = signal(false);
  protected attachments = signal<Attachment[]>([]);
  protected readonly maxAttachmentFileSize = 5 * 1024 * 1024;

  ngOnInit() {
    this.loadUsers();
  }

  ngOnChanges() {
    this.fillForm();
  }

  private loadUsers() {
    this.mailsService.getAllUsers().subscribe({
      next: (users) => {
        this.availableUsers.set(users);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load users',
        });
      },
    });
  }

  private fillForm() {
    if (!this.mailData) {
      return;
    }

    this.mailForm.patchValue({
      subject: this.mailData.subject,
      content: this.mailData.content,
    });

    // for editing, prefill the email inputs with user.email values
    this.selectedToUsers = this.mailData.to.map((user) => user.email);
    this.selectedCcUsers = this.mailData.cc.map((user) => user.email);
    this.selectedBccUsers = this.mailData.bcc.map((user) => user.email);

    this.attachments.set(this.mailData.attachments);
  }

  onFileSelect(event: FileSelectEvent) {
    this.uploadedFiles.set([...event.currentFiles]);
  }

  onFileRemove(event: FileRemoveEvent) {
    this.uploadedFiles.set(this.uploadedFiles().filter((file) => file !== event.file));
  }

  onExistingFileRemove(attachment: Attachment) {
    this.attachments.set(this.attachments().filter((att) => att.id !== attachment.id));
  }

  private recipientsNotEmpty(): boolean {
    return (
      this.selectedToUsers.length > 0 ||
      this.selectedCcUsers.length > 0 ||
      this.selectedBccUsers.length > 0
    );
  }

  private validateForm(): boolean {
    if (this.mailForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please fill in all required fields',
      });
      return false;
    }

    if (!this.recipientsNotEmpty()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Warning',
        detail: 'Please select at least one recipient',
      });
      return false;
    }

    return true;
  }

  private buildMailData(): CreateMail | UpdateMail {
    const subject = this.mailForm.get('subject')?.value || '';
    const content = this.mailForm.get('content')?.value || '';
    // We'll resolve email addresses to user IDs later via ensureAllRecipientsExist
    const mail: CreateMail = {
      subject,
      content,
      toIds: [],
      ccIds: [],
      bccIds: [],
    };
    return this.mailData
      ? { ...mail, retainedAttachmentIds: this.attachments().map((attachment) => attachment.id) }
      : mail;
  }

  private buildAttachmentData(): File[] {
    return this.uploadedFiles();
  }

  // Add email helpers for the input fallback
  private isValidEmail(email: string): boolean {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
  }

  protected addToFromInput() {
    const v = this.toInputValue.trim();
    if (!v) return;
    if (!this.isValidEmail(v)) {
      this.messageService.add({ severity: 'warn', summary: 'Invalid email', detail: `"${v}" is not a valid email address` });
      return;
    }
    if (!this.selectedToUsers.includes(v)) {
      this.selectedToUsers = [...this.selectedToUsers, v];
    }
    this.toInputValue = '';
  }

  protected addCcFromInput() {
    const v = this.ccInputValue.trim();
    if (!v) return;
    if (!this.isValidEmail(v)) {
      this.messageService.add({ severity: 'warn', summary: 'Invalid email', detail: `"${v}" is not a valid email address` });
      return;
    }
    if (!this.selectedCcUsers.includes(v)) {
      this.selectedCcUsers = [...this.selectedCcUsers, v];
    }
    this.ccInputValue = '';
  }

  protected addBccFromInput() {
    const v = this.bccInputValue.trim();
    if (!v) return;
    if (!this.isValidEmail(v)) {
      this.messageService.add({ severity: 'warn', summary: 'Invalid email', detail: `"${v}" is not a valid email address` });
      return;
    }
    if (!this.selectedBccUsers.includes(v)) {
      this.selectedBccUsers = [...this.selectedBccUsers, v];
    }
    this.bccInputValue = '';
  }

  // Remove recipient methods
  protected removeToUser(email: string) {
    this.selectedToUsers = this.selectedToUsers.filter(e => e !== email);
  }

  protected removeCcUser(email: string) {
    this.selectedCcUsers = this.selectedCcUsers.filter(e => e !== email);
  }

  protected removeBccUser(email: string) {
    this.selectedBccUsers = this.selectedBccUsers.filter(e => e !== email);
  }

  private handleMailSuccess(message: string, navigateTo: string) {
    this.messageService.add({
      severity: 'success',
      summary: 'Success',
      detail: message,
    });
    this.resetForm();
    this.isLoading.set(false);
    this.router.navigate([navigateTo]);
  }

  private handleMailError(error: HttpErrorResponse, defaultMessage: string) {
    this.isLoading.set(false);
    const errorResponse = error.error as Partial<ErrorResponse> | null;
    const errorMessage = errorResponse?.message || defaultMessage;
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
    });
  }

  async onSubmit() {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);
    if (!(await this.resolveRecipientIds(mailData))) {
      this.isLoading.set(false);
      return;
    }

    if (this.mailData) {
      this.mailsService.updateMails(this.mailData.id, mailData as UpdateMail, attachments).subscribe({
        next: (updatedMail) => {
          this.mailsService.sendMail(updatedMail.id).subscribe({
            next: () => this.handleMailSuccess('Mail sent successfully', '/mails/sent'),
            error: (error) => this.handleMailError(error, 'Failed to send mail'),
          });
        },
        error: (error) => this.handleMailError(error, 'Failed to update mail'),
      });
      return;
    }

    this.mailsService.createAndSendMail(mailData, attachments).subscribe({
      next: () => this.handleMailSuccess('Mail sent successfully', '/mails/sent'),
      error: (error) => this.handleMailError(error, 'Failed to send mail'),
    });
  }

  async onSaveDraft() {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);
    if (!(await this.resolveRecipientIds(mailData))) {
      this.isLoading.set(false);
      return;
    }

    const request = this.mailData
      ? this.mailsService.updateMails(this.mailData.id, mailData as UpdateMail, attachments)
      : this.mailsService.createDraft(mailData, attachments);
    request.subscribe({
        next: () => this.handleMailSuccess('Mail saved as draft', '/mails/drafts'),
        error: (error) => this.handleMailError(error, 'Failed to save draft'),
      });
  }

  resetForm() {
    this.mailForm.reset();
    this.selectedToUsers = [];
    this.selectedCcUsers = [];
    this.selectedBccUsers = [];
    this.toInputValue = '';
    this.ccInputValue = '';
    this.bccInputValue = '';
    this.uploadedFiles.set([]);
    this.attachments.set([]);
  }

  // Ensure that for each entered email an existing user ID exists; create users if necessary.
  private async ensureAllRecipientsExist(emailsSignal: () => string[]): Promise<string[] | null> {
    const emails = emailsSignal();
    const ids: string[] = [];

    for (const e of emails) {
      const trimmed = e?.trim();
      if (!trimmed) continue;

      const found = this.availableUsers().find((u) => u.email.toLowerCase() === trimmed.toLowerCase());
      if (found) {
        ids.push(found.id);
        continue;
      }

      // create via API
      try {
        const resp = await firstValueFrom(this.mailsService.ensureUser(trimmed));
        if (resp && resp.id) {
          ids.push(resp.id);
          // update local users cache
          this.availableUsers.set([...this.availableUsers(), { id: resp.id, firstName: '', lastName: '', email: trimmed }]);
        } else {
          this.messageService.add({ severity: 'error', summary: 'Failed', detail: `Could not ensure user for ${trimmed}` });
          return null;
        }
      } catch {
        this.messageService.add({ severity: 'error', summary: 'Failed', detail: `Could not ensure user for ${trimmed}` });
        return null;
      }
    }

    return ids;
  }

  private async resolveRecipientIds(mailData: CreateMail): Promise<boolean> {
    const [toIds, ccIds, bccIds] = await Promise.all([
      this.ensureAllRecipientsExist(() => this.selectedToUsers),
      this.ensureAllRecipientsExist(() => this.selectedCcUsers),
      this.ensureAllRecipientsExist(() => this.selectedBccUsers),
    ]);

    if (!toIds || !ccIds || !bccIds) return false;
    mailData.toIds = toIds;
    mailData.ccIds = ccIds;
    mailData.bccIds = bccIds;
    return true;
  }
}
