import {Component, inject, OnInit, signal} from '@angular/core';
import {MailsList} from '../../../components/mails/mails-list/mails-list';
import {MailsService} from '../../../services/mails/mails-service';
import {MessageService} from 'primeng/api';
import {MailListItem} from '../../../types/mails';
import {Toast} from 'primeng/toast';


@Component({
  selector: 'app-mail-inbox',
  imports: [
    MailsList,
    Toast
  ],
  templateUrl: './mail-inbox.html',
})
export class MailInbox implements OnInit {
  private mailsService = inject(MailsService);
  private messageService = inject(MessageService);

  protected mails = signal<MailListItem[]>([]);
  protected isLoading = signal(true);
  protected isLoadingMore = signal(false);
  protected isRefreshing = signal(false);
  protected hasMore = signal(false);
  private currentPage = 0;
  private readonly pageSize = 25;

  ngOnInit() {
    this.loadMails();
  }

  protected loadMore() {
    if (this.isLoadingMore() || !this.hasMore()) {
      return;
    }

    this.loadMails(this.currentPage + 1);
  }

  protected refreshInbox() {
    if (this.isRefreshing() || this.isLoading()) {
      return;
    }

    this.loadMails(0, true);
  }

  private loadMails(page = 0, isRefresh = false) {
    const isFirstPage = page === 0;
    if (isRefresh) {
      this.isRefreshing.set(true);
    } else if (isFirstPage) {
      this.isLoading.set(true);
    } else {
      this.isLoadingMore.set(true);
    }

    this.mailsService.getIncomingMails(page, this.pageSize).subscribe({
      next: (response) => {
        this.currentPage = response.page;
        this.hasMore.set(response.hasNext);
        this.mails.update((existing) => isFirstPage ? response.content : [...existing, ...response.content]);
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
        this.isRefreshing.set(false);
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Failed to Load Mails',
          detail: err.error?.message || 'An error occurred',
        });
        this.isLoading.set(false);
        this.isLoadingMore.set(false);
        this.isRefreshing.set(false);
      },
    });
  }
}
