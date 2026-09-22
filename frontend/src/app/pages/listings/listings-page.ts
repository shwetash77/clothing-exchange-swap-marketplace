import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CATEGORIES } from '../../core/config';
import { errorMessage } from '../../core/errors';
import { Item } from '../../core/models';

@Component({
  selector: 'app-listings-page',
  imports: [RouterLink],
  templateUrl: './listings-page.html',
})
export class ListingsPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly categories = CATEGORIES;
  readonly category = signal('');
  readonly items = signal<Item[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit() {
    this.load();
  }

  select(category: string) {
    this.category.set(category);
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.listItems(this.category() || undefined).subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
