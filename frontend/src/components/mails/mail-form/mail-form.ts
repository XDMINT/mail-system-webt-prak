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
import { CreateMail, Mail } from '../../../types/mails';
import { InputTextModule } from 'primeng/inputtext';
import { ImageModule } from 'primeng/image';
import { Attachment } from '../../../types/attachment';

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
    ImageModule,
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
    subject: new FormControl('', [Validators.required, Validators.maxLength(20)]),
    content: new FormControl('', [Validators.required, Validators.maxLength(500)]),
  });

  protected availableUsers = signal<User[]>([]);
  // Now hold email addresses instead of user IDs (plain arrays for ngModel)
  protected selectedToUsers: string[] = [];
  protected selectedCcUsers: string[] = [];
  protected selectedBccUsers: string[] = [];
  protected selectedReplyToUsers: string[] = [];

  // Input values for recipient fields (standalone from reactive form)
  protected toInputValue = '';
  protected ccInputValue = '';
  protected bccInputValue = '';
  protected replyToInputValue = '';

  // temporary input fields for fallback add (signals for proper change detection)
  protected uploadedFiles = signal<File[]>([]);
  protected isLoading = signal(false);
  protected attachments = signal<Attachment[]>([]);

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
    this.selectedReplyToUsers = this.mailData.replyTo.map((user) => user.email);

    this.attachments.set(this.mailData.attachments);
  }

  onFileSelect(event: FileSelectEvent) {
    this.uploadedFiles.set([...this.uploadedFiles(), ...event.files]);
  }

  onFileRemove(event: FileRemoveEvent) {
    this.uploadedFiles.set(this.uploadedFiles().filter((file) => file !== event.file));
  }

  onExistingFileRemove(attachment: Attachment) {
    this.attachments.set(this.attachments().filter((att) => att.url !== attachment.url));
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

  private buildMailData(): CreateMail | null {
    const subject = this.mailForm.get('subject')?.value || '';
    const content = this.mailForm.get('content')?.value || '';
    // We'll resolve email addresses to user IDs later via ensureAllRecipientsExist
    return {
      subject,
      content,
      toIds: [],
      ccIds: [],
      bccIds: [],
      replyToIds: [],
    };
  }

  private buildAttachmentData(): File[] {
    const newAttachments = this.uploadedFiles().map((file) => file);
    const existingAttachments = this.attachments().map((att) =>
      this.blobToFile(att.blob!, att.fileName),
    );
    return [...newAttachments, ...existingAttachments];
  }

  private blobToFile(blob: Blob, filename: string): File {
    return new File([blob], filename, { type: blob.type });
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

  protected addReplyToFromInput() {
    const v = this.replyToInputValue.trim();
    if (!v) return;
    if (!this.isValidEmail(v)) {
      this.messageService.add({ severity: 'warn', summary: 'Invalid email', detail: `"${v}" is not a valid email address` });
      return;
    }
    if (!this.selectedReplyToUsers.includes(v)) {
      this.selectedReplyToUsers = [...this.selectedReplyToUsers, v];
    }
    this.replyToInputValue = '';
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

  protected removeReplyToUser(email: string) {
    this.selectedReplyToUsers = this.selectedReplyToUsers.filter(e => e !== email);
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

  private handleMailError(error: any, defaultMessage: string) {
    this.isLoading.set(false);
    const errorMessage = error.error?.message || defaultMessage;
    this.messageService.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
    });
  }

  onSubmit() {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    if (mailData === null) return; // mapping error already shown
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);
    // Ensure external emails exist as users (create if necessary)
    this.ensureAllRecipientsExist(() => this.selectedToUsers).then((toIds) => {
      if (!toIds) { this.isLoading.set(false); return; }
      this.ensureAllRecipientsExist(() => this.selectedCcUsers).then((ccIds) => {
        if (!ccIds) { this.isLoading.set(false); return; }
        this.ensureAllRecipientsExist(() => this.selectedBccUsers).then((bccIds) => {
          if (!bccIds) { this.isLoading.set(false); return; }
          this.ensureAllRecipientsExist(() => this.selectedReplyToUsers).then((replyToIds) => {
            if (!replyToIds) { this.isLoading.set(false); return; }

            // replace mailData ids with ensured ids
            mailData.toIds = toIds;
            mailData.ccIds = ccIds;
            mailData.bccIds = bccIds;
            mailData.replyToIds = replyToIds;



          });
        });
      });
    });

    if (this.mailData) {
      this.mailsService.updateMails(this.mailData.id, mailData, attachments).subscribe({
        next: () => this.handleMailSuccess('Mail updated successfully', '/mails/drafts'),
        error: (error) => this.handleMailError(error, 'Failed to update mail'),
      });
    } else {
      this.mailsService.createAndSendMail(mailData, attachments).subscribe({
        next: () => this.handleMailSuccess('Mail sent successfully', '/mails/sent'),
        error: (error) => this.handleMailError(error, 'Failed to send mail'),
      });
    }
  }

  onSaveDraft() {
    if (!this.validateForm()) {
      return;
    }

    const mailData = this.buildMailData();
    if (mailData === null) return; // mapping error already shown to user
    const attachments = this.buildAttachmentData();
    this.isLoading.set(true);

    this.mailsService.createDraft(mailData, attachments).subscribe({
      next: () => this.handleMailSuccess('Mail saved as draft', '/mails/drafts'),
      error: (error) => this.handleMailError(error, 'Failed to save draft'),
    });
  }

  resetForm() {
    this.mailForm.reset();
    this.selectedToUsers = [];
    this.selectedCcUsers = [];
    this.selectedBccUsers = [];
    this.selectedReplyToUsers = [];
    this.toInputValue = '';
    this.ccInputValue = '';
    this.bccInputValue = '';
    this.replyToInputValue = '';
    this.uploadedFiles.set([]);
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
        const resp = await this.mailsService.ensureUser(trimmed).toPromise();
        if (resp && resp.id) {
          ids.push(resp.id);
          // update local users cache
          this.availableUsers.set([...this.availableUsers(), { id: resp.id, firstName: '', lastName: '', email: trimmed }]);
        } else {
          this.messageService.add({ severity: 'error', summary: 'Failed', detail: `Could not ensure user for ${trimmed}` });
          return null;
        }
      } catch (ex) {
        this.messageService.add({ severity: 'error', summary: 'Failed', detail: `Could not ensure user for ${trimmed}` });
        return null;
      }
    }

    return ids;
  }
}
