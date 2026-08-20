import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Mail, MailListItem } from '../../../types/mails';
import { TagModule } from 'primeng/tag';
import {getSeverityBadge} from '../../../utils/badges';

@Component({
  selector: 'app-mails-list-element',
  imports: [TagModule, RouterLink],
  templateUrl: './mails-list-element.html',
})
export class MailsListElement {
  @Input() mail!: Mail | MailListItem;

  protected readonly getSeverityBadge = getSeverityBadge;

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      });
    }
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  }

  getAttachmentCount(): number {
    if ('attachmentCount' in this.mail) {
      return this.mail.attachmentCount;
    }
    return this.mail.attachments.length;
  }

}
