import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Commande, CommandeAvecLignes, CommandeRequest } from '../models/commande.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CommandeService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl + '/api/commandes';

  getCommandes(): Observable<Commande[]> {
    return this.http.get<Commande[]>(this.apiUrl);
  }

  getCommandeById(id: number): Observable<CommandeAvecLignes> {
    return this.http.get<CommandeAvecLignes>(`${this.apiUrl}/${id}/detail`);
  }

  getCommandesByUserId(userId: string): Observable<Commande[]> {
    return this.http.get<Commande[]>(`${this.apiUrl}/user/${userId}`);
  }

  createCommande(request: CommandeRequest): Observable<Commande> {
    return this.http.post<Commande>(this.apiUrl, request);
  }

  updateCommande(id: number, request: CommandeRequest): Observable<Commande> {
    return this.http.put<Commande>(`${this.apiUrl}/${id}`, request);
  }

  annulerCommande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  passerEnCours(id: number): Observable<Commande> {
    return this.http.patch<Commande>(`${this.apiUrl}/${id}/en-cours`, {});
  }
}
