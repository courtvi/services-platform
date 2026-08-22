export interface Produit {
  id: number;
  boutiqueId: string;
  cle: string;
  origine: string;
  prix: number;
  unite: string;
  categorie: string;
  tagCle?: string;
}
