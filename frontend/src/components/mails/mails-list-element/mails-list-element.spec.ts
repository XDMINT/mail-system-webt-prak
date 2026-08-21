import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { MailsListElement } from './mails-list-element';
import { MailListItem } from '../../../types/mails';

describe('MailsListElement', () => {
  let component: MailsListElement;
  let fixture: ComponentFixture<MailsListElement>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MailsListElement],
      providers: [provideRouter([])],
    })
    .compileComponents();

    fixture = TestBed.createComponent(MailsListElement);
    component = fixture.componentInstance;
    component.mail = {
      id: 'mail-1',
      sender: {
        id: 'user-1',
        firstName: 'Ameline',
        lastName: 'Allanson',
        email: 'aallanson@example.com',
      },
      subject: 'Test subject',
      content: 'Test content',
      status: 'SENT',
      source: 'INTERN',
      createdAt: '2026-08-20T12:00:00Z',
      updatedAt: '2026-08-20T12:00:00Z',
      attachmentCount: 0,
    } as MailListItem;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
