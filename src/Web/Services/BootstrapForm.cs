using System.ComponentModel.DataAnnotations;
using FamilyFit.Contracts.Models.Authentication;
namespace FamilyFit.Web.Services;
public sealed class BootstrapForm
{
    [Required(ErrorMessage = "Saisis le code d’initialisation."), StringLength(128, MinimumLength = 16, ErrorMessage = "Le code doit contenir entre 16 et 128 caractères.")]
    public string Code { get; set; } = "";
    [Required(ErrorMessage = "Choisis un pseudo."), StringLength(40, MinimumLength = 2, ErrorMessage = "Le pseudo doit contenir entre 2 et 40 caractères.")]
    public string Nickname { get; set; } = "";
    [Range(1, 120, ErrorMessage = "Indique un âge entre 1 et 120 ans.")] public int Age { get; set; } = 18;
    [Range(10, 500, ErrorMessage = "Indique un poids entre 10 et 500 kg.")] public double WeightKg { get; set; } = 70;
    public BootstrapInput Input() => new(Code, Nickname, Age, WeightKg);
}
