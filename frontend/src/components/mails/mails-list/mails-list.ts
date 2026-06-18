import {Component, EventEmitter, Input, Output} from '@angular/core';
import { Mail, MailListItem } from '../../../types/mails';
import { MailsListElement } from '../mails-list-element/mails-list-element';

@Component({
  selector: 'app-mails-list',
  imports: [MailsListElement],
  providers: [],
  templateUrl: './mails-list.html',
})
export class MailsList {

  @Input () mails: Array<Mail | MailListItem> = [];
  @Input() isLoading = false;
  @Input() title: string = '';
  @Input() hasMore = false;
  @Input() isLoadingMore = false;
  @Output() loadMore = new EventEmitter<void>();

}
