using FamilyFit.Contracts.Models.Exercises;
using FamilyFit.Contracts.Models.Equipment;
using System.ComponentModel;
using System.Security.Claims;
using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Locations;
using FamilyFit.Contracts.Models.Members;
using FamilyFit.Contracts.Models.Programs;
using FamilyFit.Contracts.Models.Workouts;
using ModelContextProtocol.Protocol;
using ModelContextProtocol.Server;

namespace FamilyFit.Web.Mcp;

[McpServerToolType]
public sealed class FamilyWriteTools(AgentToolRunner runner, IMediator mediator)
{
    [McpServerTool(Name = "create_exercise", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Administrateur MCP uniquement. Créer un exercice manquant : id stable a-z, 0-9, _ (64 caractères), name (100), equipmentId existant (list_equipment), muscle (200), targetType time ou repetitions, instructions (2000), postureTip (1000). Refuse les identifiants existants sans les modifier. Ne déclare pas le matériel disponible dans un lieu ; consulter list_locations. Aucun média externe téléchargé.")]
    public Task<CallToolResult> CreateExercise(ClaimsPrincipal user, ExerciseInput input, CancellationToken cancellationToken)
        => runner.Run(user, () => mediator.CreateExercise(user, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "save_member", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Créer (administrateur) ou modifier un profil selon les droits du membre de la clé. Lire d’abord sa révision actuelle ; ne pas inventer le poids ni les données personnelles.")]
    public Task<CallToolResult> SaveMember(ClaimsPrincipal user, MemberInput input, CancellationToken cancellationToken, Guid? memberId = null)
        => runner.Run(user, () => mediator.SaveMember(user, memberId, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "save_location", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Créer/modifier un lieu et son matériel confirmé. Fournir le propriétaire et la révision actuelle pour une modification. Aucun inventaire de salle n’est inféré.")]
    public Task<CallToolResult> SaveLocation(ClaimsPrincipal user, LocationInput input, CancellationToken cancellationToken, Guid? locationId = null)
        => runner.Run(user, () => mediator.SaveLocation(user, locationId, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "save_equipment", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Administrateur uniquement. Créer ou modifier une fiche du catalogue : id stable (A-Z, 0-9, _, 64 caractères maximum), name (80), description (2000) et targetMuscles (1 à 12 libellés de 60 caractères). Revision=0 crée une fiche ; pour modifier, lire list_equipment et fournir sa révision actuelle. Préserve la photo et les associations aux lieux. Ne déclare pas sa disponibilité dans une salle.")]
    public Task<CallToolResult> SaveEquipment(ClaimsPrincipal user, EquipmentInput input, CancellationToken cancellationToken)
        => runner.Run(user, () => mediator.SaveEquipment(user, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "set_equipment_image", ReadOnly = false, Destructive = true, OpenWorld = false), Description("Administrateur uniquement. Envoyer ou remplacer la photo d’un équipement existant : equipmentId et input {revision, base64, contentType}. Base64 brut sans préfixe data:, image/png, image/jpeg ou image/webp, 2 Mio maximum décodés ; type MIME conforme au contenu. Pour retirer la photo : input {revision, remove:true}, sans base64/contentType. Lire list_equipment avant l’écriture et réutiliser la révision retournée pour la suivante. Retourne la fiche et son URL locale authentifiée, jamais les octets. Ne télécharge aucune URL externe.")]
    public Task<CallToolResult> SetEquipmentImage(ClaimsPrincipal user, string equipmentId, EquipmentImageInput input, CancellationToken cancellationToken)
        => runner.Run(user, () => mediator.SaveEquipmentImage(user, equipmentId, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "save_program_draft", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Administrateur uniquement. Créer/modifier un brouillon de sept dates distinctes du lundi au dimanche. Respecter le matériel, exclusions et objectifs du membre. Une copie d’un programme publié se crée avec programId absent ; cette action ne publie rien.")]
    public Task<CallToolResult> SaveDraft(ClaimsPrincipal user, ProgramInput input, CancellationToken cancellationToken, Guid? programId = null)
        => runner.Run(user, () => mediator.SaveProgram(user, programId, input, cancellationToken), cancellationToken);

    [McpServerTool(Name = "publish_program", ReadOnly = false, Destructive = false, OpenWorld = false), Description("Administrateur uniquement et sur demande explicite. Publier un brouillon validé ; archive l’ancienne version de cette semaine et rend la nouvelle prescription immuable.")]
    public Task<CallToolResult> Publish(ClaimsPrincipal user, Guid programId, CancellationToken cancellationToken)
        => runner.Run(user, () => mediator.PublishProgram(user, programId, cancellationToken), cancellationToken);

    [McpServerTool(Name = "record_workout_result", ReadOnly = false, Destructive = false, Idempotent = true, OpenWorld = false), Description("Enregistrer les performances réellement déclarées par le propriétaire de la clé pour son programme publié. Fournir un UUID stable, les horaires réels et le détail des séries. Le serveur calcule terminé/partiel/abandonné. Réutiliser le même UUID ET payload lors d’un réessai ; aucune correction destructive d’un résultat existant.")]
    public Task<CallToolResult> RecordWorkout(ClaimsPrincipal user, WorkoutSubmission input, CancellationToken cancellationToken)
        => runner.Run(user, () => mediator.SubmitWorkout(user, input, cancellationToken), cancellationToken);
}
